package com.dev.coupon.product.exeception;

import com.dev.coupon.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ProductErrorCode implements ErrorCode {
	PRODUCT_NOT_FOUND(HttpStatus.BAD_REQUEST, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다." );

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	ProductErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}
}
