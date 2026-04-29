package com.dev.coupon.coupon.service;

import com.dev.coupon.common.exception.SystemException;
import com.dev.coupon.coupon.domain.CouponIssueResult;
import com.dev.coupon.coupon.exception.RedisErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisIssueService {

	private final StringRedisTemplate redisTemplate;

	private static final String RESERVE_COUPON_LUA_SCRIPT = """
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
			  RESERVE_COUPON_LUA_SCRIPT,
			  Long.class
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
			throw new SystemException(RedisErrorCode.REDIS_ISSUE_EXECUTION_FAILED, e);
		}

		if (rawResult == null) {
			throw new SystemException(RedisErrorCode.REDIS_ISSUE_INVALID_RESULT);
		}

		if (rawResult == 1L) return CouponIssueResult.ALREADY_ISSUED;
		if (rawResult == 2L) return CouponIssueResult.SOLD_OUT;
		if (rawResult == 3L) return CouponIssueResult.SUCCESS;

		log.error("[REDIS_ISSUE_INVALID_RESULT] result={}, eventId={}, userId={}", rawResult, eventId, userId);
		throw new SystemException(RedisErrorCode.REDIS_ISSUE_INVALID_RESULT);
	}

	public void initEventStock(Long eventId, int remainingQuantity) {
		redisTemplate.opsForValue().set(stockKey(eventId), String.valueOf(remainingQuantity));
	}

	private String stockKey(Long eventId) {
		return "coupon:event:" + eventId + ":stock";
	}

	private String issuedUsersKey(Long eventId) {
		return "coupon:event:" + eventId + ":issued-users";
	}
}
