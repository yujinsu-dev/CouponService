package com.dev.coupon.coupon.service;

import com.dev.coupon.common.exception.SystemException;
import com.dev.coupon.common.util.RedisLuaScriptLoader;
import com.dev.coupon.coupon.exception.SystemErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisRecoveryService {

	private final StringRedisTemplate redisTemplate;

	private static final RedisScript<Long> RECOVERY_SCRIPT =
			  RedisLuaScriptLoader.longScript("lua/coupon/recovery_stock.lua");

	public void restoreStock(Long eventId, List<Long> userIds, int remainingQuantity) {
		List<String> args = new ArrayList<>();
		args.add(String.valueOf(remainingQuantity));

		// ARGV[1] = remainingQuantity, ARGV[2] = 첫 번째 userId, ARGV[3] = 두 번째 userId ...
		userIds.stream()
				  .map(String::valueOf)
				  .forEach(args::add);

		try {
			Long result = redisTemplate.execute(
					  RECOVERY_SCRIPT,
					  List.of(stockKey(eventId), issuedUsersKey(eventId)),
					  args.toArray()
			);

			if (!Long.valueOf(1L).equals(result)) {
				log.error("[REDIS_RECOVERY_INVALID_RESULT] result={}, eventId={}", result, eventId);
				throw new SystemException(SystemErrorCode.REDIS_RECOVERY_EXECUTION_FAILED);
			}
		} catch (SystemException e) {
			throw e;
		} catch (Exception e) {
			log.error("[REDIS_RECOVERY_EXECUTION_FAILED] eventId={}, remainingQuantity={}", eventId, remainingQuantity, e);
			throw new SystemException(SystemErrorCode.REDIS_RECOVERY_EXECUTION_FAILED, e);
		}
	}

	private String stockKey(Long eventId) {
		return "coupon:event:" + eventId + ":stock";
	}

	private String issuedUsersKey(Long eventId) {
		return "coupon:event:" + eventId + ":issued-users";
	}
}
