package com.dev.coupon.common;

import com.dev.coupon.common.exception.ErrorResponse;
import lombok.Getter;

@Getter
public class ApiResponse<T> {

	private final boolean success;
	private final T data;

	public ApiResponse(boolean success, T data) {
		this.success = success;
		this.data = data;
	}

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data);
	}


}
