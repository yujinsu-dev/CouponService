package com.dev.coupon.product.repository;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;

@Getter
public class ProductCondition {

	private String name;

	@PositiveOrZero
	private Long minPrice;

	@PositiveOrZero
	private Long maxPrice;
}
