package com.dev.coupon.coupon.controller;

import com.dev.coupon.common.ApiResponse;
import com.dev.coupon.coupon.dto.CouponEventCreateRequest;
import com.dev.coupon.coupon.dto.CouponEventResponse;
import com.dev.coupon.coupon.service.CouponEventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/coupon-events")
public class AdminCouponEventController {

	private final CouponEventService couponEventService;

	@PostMapping
	public CouponEventResponse create(@Valid @RequestBody CouponEventCreateRequest request) {
		return couponEventService.create(request);
	}

	@GetMapping
	public ApiResponse<List<CouponEventResponse>> list() {
		return ApiResponse.success(couponEventService.getCouponEvents());
	}
}
