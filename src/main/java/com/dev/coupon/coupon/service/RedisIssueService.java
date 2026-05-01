package com.dev.coupon.coupon.service;

import com.dev.coupon.common.exception.SystemException;
import com.dev.coupon.coupon.domain.CouponEvent;
import com.dev.coupon.coupon.domain.CouponIssueResult;
import com.dev.coupon.coupon.exception.CouponErrorCode;
import com.dev.coupon.coupon.exception.SystemErrorCode;
import com.dev.coupon.coupon.repository.CouponEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisIssueService {

	private final StringRedisTemplate redisTemplate;
	private final CouponEventRepository eventRepository;
	private final CouponStockResyncService resyncService;

	private static final String RESERVE_COUPON_SCRIPT = """
			  	if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
			  		return 1
			  	end
			  
			  	local stock = tonumber(redis.call('GET', KEYS[1]))
			  	if stock == nil or stock <= 0 then
			  		return 2
			  	end
			  
			  	redis.call('DECR', KEYS[1])
			  	redis.call('SADD', KEYS[2], ARGV[1])
			  	return 3
			  """;

	private static final RedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>(
			  RESERVE_COUPON_SCRIPT,
			  Long.class
	);

	// 선점된 사용자일 때만 복구 진행
	private static final String RESERVE_COUPON_ROLLBACK_SCRIPT = """
			  if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
			  	  redis.call('INCR', KEYS[1])
			  	  redis.call('SREM', KEYS[2], ARGV[1])
			  end
			  """;

	private static final RedisScript<Void> ROLLBACK_SCRIPT = new DefaultRedisScript<>(
			  RESERVE_COUPON_ROLLBACK_SCRIPT,
			  Void.class
	);

	public CouponIssueResult reserveCoupon(Long eventId, Long userId) {
		Long rawResult;

		try {
			rawResult = redisTemplate.execute(
					  RESERVE_SCRIPT,
					  List.of(stockKey(eventId), issuedUsersKey(eventId)),
					  userId.toString()
			);
		} catch (Exception e) {

			throw new SystemException(SystemErrorCode.REDIS_ISSUE_EXECUTION_FAILED, e);
		}

		if (rawResult == null) {
			throw new SystemException(SystemErrorCode.REDIS_ISSUE_INVALID_RESULT);
		}

		if (rawResult == 1L) return CouponIssueResult.ALREADY_ISSUED;
		if (rawResult == 2L) return CouponIssueResult.SOLD_OUT;
		if (rawResult == 3L) return CouponIssueResult.SUCCESS;

		log.error("[REDIS_ISSUE_INVALID_RESULT] result={}, eventId={}, userId={}", rawResult, eventId, userId);
		throw new SystemException(SystemErrorCode.REDIS_ISSUE_INVALID_RESULT);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void reserveRollback(Long eventId, Long userId) {
		CouponEvent event = eventRepository.findById(eventId).orElseThrow();

		try {
			redisTemplate.execute(
					  ROLLBACK_SCRIPT,
					  List.of(stockKey(eventId), issuedUsersKey(eventId)),
					  userId.toString()
			);
		}
		catch (Exception e) {
			// DB, Redis 둘다 실패 케이스 마킹
			resyncService.markPending(eventId);
			throw new SystemException(SystemErrorCode.COUPON_ISSUE_COMPENSATION_FAILED, e);
		}
	}

	public void initEventStock(Long eventId, int remainingQuantity) {
		Boolean initalized = redisTemplate.opsForValue()
				  .setIfAbsent(stockKey(eventId), String.valueOf(remainingQuantity));

		if (!Boolean.TRUE.equals(initalized)) {
			throw new SystemException(CouponErrorCode.REDIS_STOCK_ALREADY_INITIALIZED);
		}

	}

	private String stockKey(Long eventId) {
		return "coupon:event:" + eventId + ":stock";
	}

	private String issuedUsersKey(Long eventId) {
		return "coupon:event:" + eventId + ":issued-users";
	}
}
