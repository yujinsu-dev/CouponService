package com.dev.coupon.coupon.exception;

import com.dev.coupon.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CouponErrorCode implements ErrorCode {
	FIXED_AMOUNT_MAX_DISCOUNT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "FIXED_AMOUNT_MAX_DISCOUNT_NOT_ALLOWED", "고정 금액 할인 쿠폰은 maxDiscountAmount를 설정 할 수 없습니다."),
	PERCENTAGE_MAX_DISCOUNT_REQUIRED(HttpStatus.BAD_REQUEST,"PERCENTAGE_MAX_DISCOUNT_REQUIRED", "최대 할인 금액은 필수이며 1원 이상이어야 합니다."),
	INVALID_PERCENTAGE_DISCOUNT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_PERCENTAGE_DISCOUNT_VALUE", "할인율은 1% 이상 100% 이하여야 합니다."),
	INVALID_ISSUE_START_AT(HttpStatus.BAD_REQUEST, "INVALID_ISSUE_START_AT", "쿠폰 시작일시는 현재 시각보다 이전일 수 없습니다."),
	INVALID_ISSUE_END_AT(HttpStatus.BAD_REQUEST, "INVALID_ISSUE_END_AT", "쿠폰 만료일시는 시작일시보다 이후여야 합니다."),
	COUPON_NOT_USABLE(HttpStatus.BAD_REQUEST, "COUPON_NOT_USABLE", "사용 가능한 쿠폰이 아닙니다." ),
	COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "COUPON_EXPIRED", "만료된 쿠폰입니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	CouponErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}
}
