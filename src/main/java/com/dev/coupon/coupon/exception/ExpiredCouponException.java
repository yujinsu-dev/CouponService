package com.dev.coupon.coupon.exception;

import com.dev.coupon.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;

public class ExpiredCouponException extends BusinessException {

	public ExpiredCouponException() {
		super(CouponErrorCode.COUPON_EXPIRED);
	}
}
