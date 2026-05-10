package com.dev.coupon.product.controller;

import com.dev.coupon.common.ApiResponse;
import com.dev.coupon.common.PageResponse;
import com.dev.coupon.product.dto.ProductCreateRequest;
import com.dev.coupon.product.dto.ProductResponse;
import com.dev.coupon.product.dto.ProductCondition;
import com.dev.coupon.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/products")
public class AdminProductController {

	private final ProductService productService;

	@PostMapping
	public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductCreateRequest request) {
		return ResponseEntity
				  .status(HttpStatus.CREATED)
				  .body(ApiResponse.success(productService.create(request)));
	}

	@GetMapping
	public PageResponse<ProductResponse> search(@Valid ProductCondition condition, Pageable pageable) {
		return productService.search(condition, pageable);
	}
}
