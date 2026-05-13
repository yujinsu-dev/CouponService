package com.dev.coupon.coupon.service;

import com.dev.coupon.common.exception.BusinessException;
import com.dev.coupon.common.exception.SystemException;
import com.dev.coupon.coupon.domain.*;
import com.dev.coupon.coupon.exception.CouponErrorCode;
import com.dev.coupon.coupon.exception.SystemErrorCode;
import com.dev.coupon.coupon.repository.CouponEventRepository;
import com.dev.coupon.coupon.repository.CouponIssueRepository;
import com.dev.coupon.user.domain.User;
import com.dev.coupon.user.exception.UserErrorCode;
import com.dev.coupon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class CouponIssueService {

	private final CouponIssueRepository issueRepository;
	private final CouponEventRepository eventRepository;
	private final UserRepository userRepository;
	private final RedisIssueService redisIssueService;
	private final CouponStockResyncService resyncService;
	private static final String COUPON_ISSUE_UNIQUE_CONSTRAINT = "uk_coupon_issue_event_user";

	@Transactional
	public Long issueCoupon(Long couponEventId, Long userId) {
		// Redis Lua로 발급 가능 여부 검증과 재고 선점을 원자적으로 처리
		CouponIssueResult issueResult = redisIssueService.reserveCouponIssue(couponEventId, userId);

		throwIfRedisIssueFailed(issueResult);

		AtomicBoolean requiresResync = new AtomicBoolean(false);
		registerRedisCompensationAfterCompletion(couponEventId, userId, requiresResync);

		try {
			User findUser = findUser(userId);
			CouponEvent findEvent = findEvent(couponEventId);

			validateCouponIssue(findEvent);

			int decreasedStockCount = eventRepository.decreaseStockIfAvailable(couponEventId);

			if (decreasedStockCount == 0) {
				log.error("[COUPON_STOCK_INCONSISTENT] eventId = {}, userId = {}", couponEventId, userId);
				requiresResync.set(true);
				throw new SystemException(SystemErrorCode.COUPON_STOCK_INCONSISTENT);
			}

			// Redis 선점 후 트랜잭션이 롤백되면 afterCompletion에서 Redis 보상 처리
			CouponIssue couponIssue = issueRepository.save(new CouponIssue(
					  findEvent,
					  findUser,
					  IssueStatus.ISSUED,
					  LocalDateTime.now(),
					  null
			));
			issueRepository.flush();

			return couponIssue.getId();
		} catch (BusinessException | SystemException e) {
			throw e;
		} catch (DataIntegrityViolationException e) {
			// 데이터 무결성 에러 발생 시 DB유니크 제약조건 위배하면 비즈니스 에러로 치환, 그 외 저장 실패(레디스 롤백)
			if (isCouponIssueUniqueViolation(e)) {
				requiresResync.set(true);
				log.warn("[COUPON_ALREADY_ISSUE_DB_DUPLICATE] eventId = {}, userId = {} ",
						  couponEventId,
						  userId
				);
				throw new BusinessException(CouponErrorCode.COUPON_ALREADY_ISSUE);
			}

			log.error("[PERSIST_FAILED] eventId = {}, userId = {}", couponEventId, userId, e);
			throw new SystemException(SystemErrorCode.COUPON_ISSUE_PERSIST_FAILED, e);
		} catch (Exception e) {
			log.error("[PERSIST_FAILED] eventId = {}, userId = {}", couponEventId, userId, e);
			throw new SystemException(SystemErrorCode.COUPON_ISSUE_PERSIST_FAILED, e);
		}
	}

	private void registerRedisCompensationAfterCompletion(
			  Long couponEventId,
			  Long userId,
			  AtomicBoolean requiresResync
	) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status == TransactionSynchronization.STATUS_COMMITTED) {
					return;
				}

				// 일반적인 실패가 아니라 redis, db둘다 신뢰하기 어려운 상황
				if (status == TransactionSynchronization.STATUS_UNKNOWN || requiresResync.get()) {
					resyncService.markPending(couponEventId);
					return;
				}

				// redis 선점에 성공했으나, DB 트랜잭션 rollback 되고 resync가 아닌 경우 STATUS_ROLLED_BACK
				redisIssueService.reserveRollback(couponEventId, userId);
			}
		});
	}

	private CouponEvent findEvent(Long couponEventId) {
		return eventRepository.findById(couponEventId)
				  .orElseThrow(() -> new BusinessException(CouponErrorCode.COUPON_EVENT_NOT_FOUND));
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
	}

	private boolean isCouponIssueUniqueViolation(DataIntegrityViolationException exception) {
		Throwable cause = exception;

		//DataIntegrityViolationException으로 감싸진 예외를 순회해서 DB유니크 제약 위반인지 판단한다.
		while (cause != null) {
			if (cause instanceof ConstraintViolationException constraintViolationException) {
				String constraintName = constraintViolationException.getConstraintName();
				if(isCouponIssueConstraint(constraintName))
					return true;
			}

			if (cause instanceof SQLException sqlException) {
				if (isMySqlDuplicateKey(sqlException) && containsConstrainName(sqlException.getMessage())) {
					return true;
				}
			}

			cause = cause.getCause();
		}

		return false;
	}

	private boolean isCouponIssueConstraint(String constraintName) {
		return COUPON_ISSUE_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintName);
	}

	private boolean isMySqlDuplicateKey(SQLException sqlException) {
		return sqlException.getErrorCode() == 1062;
	}

	private boolean containsConstrainName(String message) {
		return message != null
				  && message.toLowerCase().contains(COUPON_ISSUE_UNIQUE_CONSTRAINT.toLowerCase());
	}

	private void validateCouponIssue(CouponEvent findEvent) {
		LocalDateTime issueStartAt = findEvent.getIssueStartAt();
		LocalDateTime issueEndAt = findEvent.getIssueEndAt();
		LocalDateTime now = LocalDateTime.now();

		if (findEvent.isStockResyncPending()) {
			throw new BusinessException(CouponErrorCode.COUPON_NOT_ISSUABLE);
		}

		if (findEvent.getStatus() != EventStatus.OPEN) {
			throw new BusinessException(CouponErrorCode.COUPON_NOT_ISSUABLE);
		}

		if (issueStartAt.isAfter(now) || !issueEndAt.isAfter(now)) {
			throw new BusinessException(CouponErrorCode.COUPON_NOT_ISSUABLE);
		}
	}

	private void throwIfRedisIssueFailed(CouponIssueResult issueResult) {
		if (issueResult == CouponIssueResult.ALREADY_ISSUED) {
			throw new BusinessException(CouponErrorCode.COUPON_ALREADY_ISSUE);
		}

		if (issueResult == CouponIssueResult.SOLD_OUT) {
			throw new BusinessException(CouponErrorCode.COUPON_SOLD_OUT);
		}

		if (issueResult == CouponIssueResult.NOT_READY) {
			throw new BusinessException(CouponErrorCode.COUPON_NOT_ISSUABLE);
		}

		if (issueResult == CouponIssueResult.NOT_STARTED) {
			throw new BusinessException(CouponErrorCode.COUPON_NOT_ISSUABLE);
		}

		if (issueResult == CouponIssueResult.ENDED) {
			throw new BusinessException(CouponErrorCode.COUPON_NOT_ISSUABLE);
		}
	}
}
