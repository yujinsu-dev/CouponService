package com.dev.coupon.user.dto;

import com.dev.coupon.coupon.domain.DiscountType;
import com.dev.coupon.coupon.domain.IssueStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class MyCouponListResponse {

	private final Long couponEventId;
	private final Long couponIssueId;
	private final String couponName;
	private final IssueStatus issueStatus;
	private final DiscountType discountType;
	private final Long discountValue;
	private final Long maxDiscountAmount;
	private final LocalDateTime expiredAt;

	public MyCouponListResponse(
			  Long couponEventId,
			  Long couponIssueId,
			  String couponName,
			  IssueStatus issueStatus,
			  DiscountType discountType,
			  Long discountValue,
			  Long maxDiscountAmount,
			  LocalDateTime expiredAt
	) {
		this.couponEventId = couponEventId;
		this.couponIssueId = couponIssueId;
		this.couponName = couponName;
		this.issueStatus = issueStatus;
		this.discountType = discountType;
		this.discountValue = discountValue;
		this.maxDiscountAmount = maxDiscountAmount;
		this.expiredAt = expiredAt;
	}
}
