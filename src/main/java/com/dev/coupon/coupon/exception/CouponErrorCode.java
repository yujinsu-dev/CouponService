package com.dev.coupon.coupon.exception;

import com.dev.coupon.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CouponErrorCode implements ErrorCode {
	FIXED_AMOUNT_MAX_DISCOUNT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "FIXED_AMOUNT_MAX_DISCOUNT_NOT_ALLOWED", "고정 금액 할인 쿠폰은 최대 할인 금액을 설정할 수 없습니다."),
	PERCENTAGE_MAX_DISCOUNT_REQUIRED(HttpStatus.BAD_REQUEST,"PERCENTAGE_MAX_DISCOUNT_REQUIRED", "최대 할인 금액은 필수이며 1원 이상이어야 합니다."),
	INVALID_PERCENTAGE_DISCOUNT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_PERCENTAGE_DISCOUNT_VALUE", "할인율은 1% 이상 100% 이하여야 합니다."),
	INVALID_ISSUE_START_AT(HttpStatus.BAD_REQUEST, "INVALID_ISSUE_START_AT", "쿠폰 시작 일시는 현재 시각보다 이후여야 합니다."),
	INVALID_ISSUE_END_AT(HttpStatus.BAD_REQUEST, "INVALID_ISSUE_END_AT", "쿠폰 만료 일시는 시작 일시보다 이후여야 합니다."),
	COUPON_NOT_USABLE(HttpStatus.BAD_REQUEST, "COUPON_NOT_USABLE", "사용할 수 없는 쿠폰입니다." ),
	COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "COUPON_EXPIRED", "만료된 쿠폰입니다."),
	COUPON_ALREADY_USED(HttpStatus.BAD_REQUEST, "COUPON_ALREADY_USED", "이미 사용된 쿠폰입니다." ),
	COUPON_ISSUE_NOT_FOUND(HttpStatus.BAD_REQUEST, "COUPON_ISSUE_NOT_FOUND", "발급된 쿠폰을 찾을 수 없습니다." ),
	COUPON_EVENT_NOT_FOUND(HttpStatus.BAD_REQUEST, "COUPON_EVENT_NOT_FOUND", "쿠폰 이벤트를 찾을 수 없습니다." ),
	COUPON_NOT_ISSUABLE(HttpStatus.BAD_REQUEST, "COUPON_NOT_ISSUABLE", "지금은 발급할 수 없는 쿠폰입니다."),
	INVALID_COUPON_EVENT_SEARCH_CONDITION(HttpStatus.BAD_REQUEST, "INVALID_COUPON_EVENT_SEARCH_CONDITION", "쿠폰 이벤트 검색 기간이 올바르지 않습니다." );

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	CouponErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}
}
