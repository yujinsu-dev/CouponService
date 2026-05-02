package com.dev.coupon.common.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class RedisLuaScriptLoader {

	private RedisLuaScriptLoader() {
	}

	public static RedisScript<Long> longScript(String path) {
		return script(path, Long.class);
	}

	public static RedisScript<String> stringScript(String path) {
		return script(path, String.class);
	}

	public static RedisScript<Void> voidScript(String path) {
		return script(path, Void.class);
	}

	private static <T> RedisScript<T> script(String path, Class<T> resultType) {
		String scriptText = readScript(path);
		return new DefaultRedisScript<>(scriptText, resultType);
	}

	private static String readScript(String path) {
		ClassPathResource resource = new ClassPathResource(path);

		try (InputStream inputStream = resource.getInputStream()) {
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load lua script: " + path, e);
		}
	}
}
