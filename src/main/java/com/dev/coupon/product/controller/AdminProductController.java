package com.dev.coupon.product.controller;

import com.dev.coupon.product.dto.PageResponse;
import com.dev.coupon.product.dto.ProductCreateRequest;
import com.dev.coupon.product.dto.ProductResponse;
import com.dev.coupon.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/products")
public class AdminProductController {

	private final ProductService productService;

	@PostMapping
	public ProductResponse create(@Valid @RequestBody ProductCreateRequest request) {
		return productService.create(request);
	}

	@GetMapping
	public PageResponse<ProductResponse> list(Pageable pageable) {
		return productService.findAll(pageable);
	}
}
