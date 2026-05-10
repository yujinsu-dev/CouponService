package com.dev.coupon.coupon.exception;

import com.dev.coupon.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SystemErrorCode implements ErrorCode {
	REDIS_ISSUE_INVALID_RESULT(HttpStatus.INTERNAL_SERVER_ERROR, "REDIS_ISSUE_INVALID_RESULT" ,
			  "쿠폰 예약 처리 중 Redis 스크립트 결과가 유효하지 않습니다."),
	REDIS_ISSUE_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "REDIS_ISSUE_EXECUTION_FAILED",
			  "쿠폰 예약 처리 중 Redis 스크립트 실행에 실패했습니다."),
	COUPON_ISSUE_PERSIST_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "COUPON_ISSUE_PERSIST_FAILED",
			  "쿠폰 발급 처리 중 오류가 발생했습니다."),
	COUPON_ISSUE_COMPENSATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "COUPON_ISSUE_COMPENSATION_FAILED",
			  "쿠폰 발급 복구 처리 중 오류가 발생했습니다."),
	COUPON_STOCK_INCONSISTENT(HttpStatus.INTERNAL_SERVER_ERROR, "INVALID_COUPON_STOCK_STATE",
			  "쿠폰 발급 데이터 정합성이 맞지 않습니다."),
	REDIS_RECOVERY_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "REDIS_RECOVERY_EXECUTION_FAILED",
			  "Redis 복구 처리 중 오류가 발생했습니다."),
	COUPON_STOCK_RESYNC_COMPLETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "COUPON_STOCK_RESYNC_COMPLETE_FAILED",
			  "쿠폰 재고 복구 완료 처리 중 오류가 발생했습니다."),
	REDIS_STOCK_ALREADY_INITIALIZED(HttpStatus.INTERNAL_SERVER_ERROR, "REDIS_STOCK_ALREADY_INITIALIZED",
			  "재고 초기화가 이미 완료되었습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	SystemErrorCode(HttpStatus httpStatus, String code, String message) {
		this.httpStatus = httpStatus;
		this.code = code;
		this.message = message;
	}
}
