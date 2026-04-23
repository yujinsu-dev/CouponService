package com.dev.coupon.coupon.controller;

import com.dev.coupon.common.ApiResponse;
import com.dev.coupon.coupon.dto.CouponUseRequest;
import com.dev.coupon.coupon.service.CouponUseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CouponUseController {

	private final CouponUseService useService;

	@PostMapping("/api/coupons/{couponIssueId}/use")
	public ApiResponse<Void> useCoupon(
			  @PathVariable Long couponIssueId,
			  @Valid @RequestBody CouponUseRequest request) {
		useService.useCoupon(couponIssueId, request.getUserId(), request.getProductId());
		return ApiResponse.success(null);
	}
}
