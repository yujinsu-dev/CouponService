package com.dev.coupon.coupon.domain;

import com.dev.coupon.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "coupon_event")

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponEvent extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private EventStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private DiscountType discountType;

	@Column(nullable = false)
	private Long discountValue;

	private Long maxDiscountAmount;

	@Column(nullable = false)
	private int totalQuantity;

	@Column(nullable = false)
	private LocalDateTime issueStartAt;

	@Column(nullable = false)
	private LocalDateTime issueEndAt;

	@Column(nullable = false)
	private boolean stockResyncPending;

	@Builder
	public CouponEvent(String name, EventStatus status, DiscountType discountType, Long discountValue,
							 Long maxDiscountAmount, int totalQuantity, LocalDateTime issueStartAt,
							 LocalDateTime issueEndAt) {
		this.name = name;
		this.status = status;
		this.discountType = discountType;
		this.discountValue = discountValue;
		this.maxDiscountAmount = maxDiscountAmount;
		this.totalQuantity = totalQuantity;
		this.issueStartAt = issueStartAt;
		this.issueEndAt = issueEndAt;
		this.stockResyncPending = false;
	}
}
