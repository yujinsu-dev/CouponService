package com.dev.coupon.product.repository;

import com.dev.coupon.product.dto.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductQueryRepository {
	Page<ProductResponse> search(ProductCondition condition, Pageable pageable);
}
