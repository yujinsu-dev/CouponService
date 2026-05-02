package com.dev.coupon.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalException {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handlerBusinessException(BusinessException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		log.warn("[BUSINESS] code = {}, message = {}", errorCode.getCode(), errorCode.getMessage());
		return ResponseEntity.status(errorCode.getHttpStatus())
				  .body(new ErrorResponse(errorCode.getCode(), errorCode.getMessage()));
	}

	@ExceptionHandler(SystemException.class)
	public ResponseEntity<ErrorResponse> handlerSystemException(SystemException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		log.error("[SYSTEM] code = {}, message = {}", errorCode.getCode(), errorCode.getMessage(), exception);
		return ResponseEntity.status(errorCode.getHttpStatus())
				  .body(new ErrorResponse(errorCode.getCode(), errorCode.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handlerMethodArumentNotValidException(MethodArgumentNotValidException exception) {
		log.warn("[VALIDATION] message = {}", exception.getMessage());
		return ResponseEntity.badRequest()
				  .body(new ErrorResponse("INVALID_REQUEST", "요청 값이 올바르지 않습니다."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handlerException(Exception exception) {
		log.error("[UNEXPECTED] message = {}", exception.getMessage(), exception);

		return ResponseEntity.internalServerError()
				  .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
	}
}
