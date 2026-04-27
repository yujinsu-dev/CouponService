package com.dev.coupon.user.controller;

import com.dev.coupon.common.ApiResponse;
import com.dev.coupon.common.PageResponse;
import com.dev.coupon.user.dto.CouponPageRequest;
import com.dev.coupon.coupon.dto.UserCouponResponse;
import com.dev.coupon.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping("/{userId}/coupons")
	public ApiResponse<PageResponse<UserCouponResponse>> getMyCoupons(
			  @PathVariable @Positive Long userId,
			  @Valid CouponPageRequest request
	) {
		return ApiResponse.success(userService.getMyUsableCoupons(userId, request));
	}
}
