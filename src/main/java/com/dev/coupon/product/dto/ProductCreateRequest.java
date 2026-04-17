package com.dev.coupon.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;

@Getter
public class ProductCreateRequest {

	@NotBlank
	private String name;

	@NotNull
	@PositiveOrZero
	private Long price;
}
