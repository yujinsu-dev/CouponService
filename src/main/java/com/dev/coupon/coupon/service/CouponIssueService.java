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

import java.sql.SQLException;
import java.time.LocalDateTime;

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
		User findUser = userRepository.findById(userId)
				  .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		CouponEvent findEvent = eventRepository.findById(couponEventId)
				  .orElseThrow(() -> new BusinessException(CouponErrorCode.COUPON_EVENT_NOT_FOUND));

		validateCouponIssue(findEvent);
		
		// Redis로 중복 발급 여부와 재고 차감을 원자적으로 선점
		CouponIssueResult issueResult = redisIssueService.reserveCoupon(findEvent.getId(), findUser.getId());
		validateRedisIssueResult(issueResult);

		eventRepository.decreaseStockIfAvailable(couponEventId);

		try {
			// Redis 선점 후 DB 저장이 실패하면 Redis 롤백 처리 redisIssueService.reserveRollback()
			CouponIssue couponIssue = issueRepository.save(new CouponIssue(
					  findEvent,
					  findUser,
					  IssueStatus.ISSUED,
					  LocalDateTime.now(),
					  null
			));
			issueRepository.flush();
			return couponIssue.getId();

		} catch (DataIntegrityViolationException e) {
			// 데이터 무결성 에러 발생 시 DB유니크 제약조건 위배하면 비즈니스 에러로 치환, 그 외 저장 실패(레디스 롤백)
			if (isCouponIssueUniqueViolation(e)) {
				resyncService.markPending(findEvent.getId());
				log.warn("[COUPON_ALREADY_ISSUE_DB_DUPLICATE] eventId={}, userId={}, fallback=db_unique_constraint",
						  findEvent.getId(),
						  findUser.getId()
				);
				throw new BusinessException(CouponErrorCode.COUPON_ALREADY_ISSUE);
			}

			log.error("[PERSIST_FAILED] eventId = {}, userId = {}", findEvent.getId(), findUser.getId(), e);
			redisIssueService.reserveRollback(findEvent.getId(), findUser.getId());
			throw new SystemException(SystemErrorCode.COUPON_ISSUE_PERSIST_FAILED, e);
		} catch (Exception e) {
			log.error("[PERSIST_FAILED] eventId = {}, userId = {}", findEvent.getId(), findUser.getId(), e);
			redisIssueService.reserveRollback(findEvent.getId(), findUser.getId());
			throw new SystemException(SystemErrorCode.COUPON_ISSUE_PERSIST_FAILED, e);
		}
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

	private void validateRedisIssueResult(CouponIssueResult issueResult) {
		if (issueResult == CouponIssueResult.ALREADY_ISSUED) {
			throw new BusinessException(CouponErrorCode.COUPON_ALREADY_ISSUE);
		}

		if (issueResult == CouponIssueResult.SOLD_OUT) {
			throw new BusinessException(CouponErrorCode.COUPON_SOLD_OUT);
		}
	}
}
