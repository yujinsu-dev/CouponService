package com.dev.coupon.product.dto;

import com.dev.coupon.product.domain.Product;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
public class ProductResponse {

	private final Long id;
	private final String name;
	private final Long price;

	public ProductResponse(Long id, String name, Long price) {
		this.id = id;
		this.name = name;
		this.price = price;
	}

	public static ProductResponse from(Product product) {
		return new ProductResponse(
				  product.getId(),
				  product.getName(),
				  product.getPrice()
		);
	}


}
