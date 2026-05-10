package com.dev.coupon.common.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
	public ResponseEntity<ErrorResponse> handlerMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
		log.warn("[VALIDATION] message = {}", exception.getMessage());
		return invalidRequest();
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handlerConstraintViolationException(ConstraintViolationException exception) {
		log.warn("[VALIDATION] message = {}", exception.getMessage());
		return invalidRequest();
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ErrorResponse> handlerMethodValidationException(HandlerMethodValidationException exception) {
		log.warn("[VALIDATION] message = {}", exception.getMessage());
		return invalidRequest();
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handlerMethodArgumentTypeMismatchException(
			  MethodArgumentTypeMismatchException exception
	) {
		log.warn(
				  "[TYPE_MISMATCH] name = {}, value = {}, requiredType = {}",
				  exception.getName(),
				  exception.getValue(),
				  exception.getRequiredType() == null ? null : exception.getRequiredType().getSimpleName()
		);
		return invalidRequest();
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handlerIllegalArgumentException(IllegalArgumentException exception) {
		log.warn("[ILLEGAL_ARGUMENT] message = {}", exception.getMessage());
		return invalidRequest();
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handlerException(Exception exception) {
		log.error("[UNEXPECTED] message = {}", exception.getMessage(), exception);

		return ResponseEntity.internalServerError()
				  .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."));
	}

	private ResponseEntity<ErrorResponse> invalidRequest() {
		return ResponseEntity.badRequest()
				  .body(new ErrorResponse("INVALID_REQUEST", "요청 값이 올바르지 않습니다."));
	}
}
