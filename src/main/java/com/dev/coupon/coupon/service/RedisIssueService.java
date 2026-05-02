package com.dev.coupon.coupon.service;

import com.dev.coupon.common.exception.SystemException;
import com.dev.coupon.common.util.CouponRedisKey;
import com.dev.coupon.common.util.RedisLuaScriptLoader;
import com.dev.coupon.coupon.domain.CouponIssueResult;
import com.dev.coupon.coupon.exception.CouponErrorCode;
import com.dev.coupon.coupon.exception.SystemErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisIssueService {

	private final StringRedisTemplate redisTemplate;
	private final CouponStockResyncService resyncService;

	private static final RedisScript<String> RESERVE_SCRIPT =
			  RedisLuaScriptLoader.stringScript("lua/coupon/reserve_coupon.lua");

	// 선점된 사용자일 때만 복구 진행
	private static final RedisScript<Void> ROLLBACK_SCRIPT =
			  RedisLuaScriptLoader.voidScript("lua/coupon/reserve_coupon_rollback.lua");

	public CouponIssueResult reserveCouponIssue(Long eventId, Long userId) {
		long nowMillis = System.currentTimeMillis();

		String issueResultStatus;

		try {
			issueResultStatus = redisTemplate.execute(
					  RESERVE_SCRIPT,
					  List.of(
								 CouponRedisKey.stock(eventId),
								 CouponRedisKey.issuedUsers(eventId),
								 CouponRedisKey.issueStartAt(eventId),
								 CouponRedisKey.issueEndAt(eventId)
					  ),
					  userId.toString(),
					  String.valueOf(nowMillis)
			);
		} catch (Exception e) {
			throw new SystemException(SystemErrorCode.REDIS_ISSUE_EXECUTION_FAILED, e);
		}

		if (issueResultStatus == null) {
			throw new SystemException(SystemErrorCode.REDIS_ISSUE_INVALID_RESULT);
		}

		try {
			return CouponIssueResult.valueOf(issueResultStatus);
		} catch (IllegalArgumentException e) {
			log.error("[REDIS_ISSUE_INVALID_RESULT] result={}, eventId={}, userId={}", issueResultStatus, eventId, userId);
			throw new SystemException(SystemErrorCode.REDIS_ISSUE_INVALID_RESULT, e);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void reserveRollback(Long eventId, Long userId) {
		try {
			redisTemplate.execute(
					  ROLLBACK_SCRIPT,
					  List.of(CouponRedisKey.stock(eventId), CouponRedisKey.issuedUsers(eventId)),
					  userId.toString()
			);
		}
		catch (Exception e) {
			// DB, Redis 둘다 실패 케이스 마킹
			resyncService.markPending(eventId);
			throw new SystemException(SystemErrorCode.COUPON_ISSUE_COMPENSATION_FAILED, e);
		}
	}

	public void initEventIssueState(
			  Long eventId,
			  int remainingQuantity,
			  LocalDateTime issueStartAt,
			  LocalDateTime issueEndAt
	) {
		long startAt = toEpochMillis(issueStartAt);
		long endAt = toEpochMillis(issueEndAt);

		Boolean initalized = redisTemplate.opsForValue()
				  .setIfAbsent(CouponRedisKey.stock(eventId), String.valueOf(remainingQuantity));

		redisTemplate.opsForValue()
				  .setIfAbsent(CouponRedisKey.issueStartAt(eventId), String.valueOf(startAt));

		redisTemplate.opsForValue()
				  .setIfAbsent(CouponRedisKey.issueEndAt(eventId), String.valueOf(endAt));

		if (!Boolean.TRUE.equals(initalized)) {
			throw new SystemException(CouponErrorCode.REDIS_STOCK_ALREADY_INITIALIZED);
		}

	}

	private long toEpochMillis(LocalDateTime issueStartAt) {
		return issueStartAt.atZone(ZoneId.of("Asia/Seoul"))
				  .toInstant()
				  .toEpochMilli();
	}

}
