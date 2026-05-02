package com.dev.coupon.common.util;

public final class CouponRedisKey {

	private static final String PREFIX = "coupon";

	private CouponRedisKey() {
	}

	public static String stock(Long couponEventId) {
		return PREFIX + ":event:" + couponEventId + ":stock";
	}

	public static String issuedUsers(Long couponEventId) {
		return PREFIX + ":event:" + couponEventId + ":issued-users";
	}

	public static String issueStream() {
		return PREFIX + ":issue:stream";
	}

	public static String issueConsumerGroup() {
		return PREFIX + ":issue:group";
	}
}
