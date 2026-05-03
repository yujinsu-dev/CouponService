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

	public static String issueStartAt(Long couponEventId) {
		return PREFIX + ":event:" + couponEventId + ":issue-start-at";
	}

	public static String issueEndAt(Long couponEventId) {
		return PREFIX + ":event:" + couponEventId + ":issue-end-at";
	}
}
