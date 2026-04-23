package com.dev.coupon.coupon.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
public class CouponUseRequest {

	@NotNull
	@Positive
	private Long userId;

	@NotNull
	@Positive
	private Long productId;

}
