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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class CouponIssueService {

	private final CouponIssueRepository issueRepository;
	private final CouponEventRepository eventRepository;
	private final UserRepository userRepository;
	private final RedisIssueService redisIssueService;

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

		try{
		// Redis 선점 후 DB 저장이 실패하면 Redis 롤백 처리
			CouponIssue couponIssue = issueRepository.save(new CouponIssue(
					  findEvent,
					  findUser,
					  IssueStatus.ISSUED,
					  LocalDateTime.now(),
					  null
			));
			return couponIssue.getId();
		} catch(Exception e) {
			log.error("[PERSIST_FAILED] eventId = {}, userId = {}", findEvent.getId(), findUser.getId(), e);
			redisIssueService.reserveRollback(findEvent.getId(), findUser.getId());
			throw new SystemException(SystemErrorCode.COUPON_ISSUE_PERSIST_FAILED, e);
		}
	}

	private void validateCouponIssue(CouponEvent findEvent) {
		LocalDateTime issueStartAt = findEvent.getIssueStartAt();
		LocalDateTime issueEndAt = findEvent.getIssueEndAt();
		LocalDateTime now = LocalDateTime.now();

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
