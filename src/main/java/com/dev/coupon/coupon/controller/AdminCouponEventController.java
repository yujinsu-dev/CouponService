package com.dev.coupon.coupon.controller;

import com.dev.coupon.common.ApiResponse;
import com.dev.coupon.coupon.domain.DiscountType;
import com.dev.coupon.coupon.domain.EventStatus;
import com.dev.coupon.coupon.dto.CouponEventCreateRequest;
import com.dev.coupon.coupon.dto.CouponEventResponse;
import com.dev.coupon.coupon.dto.condition.CouponEventCondition;
import com.dev.coupon.coupon.service.CouponEventService;
import com.dev.coupon.coupon.service.CouponStockRecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/coupon-events")
public class AdminCouponEventController {

	private final CouponEventService couponEventService;
	private final CouponStockRecoveryService stockRecoveryService;

	@PostMapping
	public ResponseEntity<ApiResponse<CouponEventResponse>> create(@Valid @RequestBody CouponEventCreateRequest request) {
		return ResponseEntity
				  .status(HttpStatus.CREATED)
				  .body(ApiResponse.success(couponEventService.create(request)));
	}

	@GetMapping
	public ApiResponse<List<CouponEventResponse>> search(
			  @RequestParam(required = false) EventStatus eventStatus,
			  @RequestParam(required = false) DiscountType discountType,
			  @RequestParam(required = false) String keyword,
			  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchStartAt,
			  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate searchEndAt
	) {
		CouponEventCondition condition = new CouponEventCondition(
				  eventStatus,
				  discountType,
				  keyword,
				  searchStartAt,
				  searchEndAt
		);

		return ApiResponse.success(couponEventService.search(condition));
	}

	@PostMapping("/{eventId}/resync")
	public ApiResponse<Void> resyncCouponEventStock(@PathVariable Long eventId) {
		stockRecoveryService.resync(eventId);
		return ApiResponse.success(null);
	}
}
