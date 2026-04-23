package com.dev.coupon.coupon.domain;

import com.dev.coupon.common.BaseEntity;
import com.dev.coupon.product.domain.Product;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "coupon_use_history")
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class CouponUseHistory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "coupon_issue_id", nullable = false)
	private CouponIssue couponIssue;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(nullable = false)
	private Long originalPrice;

	@Column(nullable = false)
	private Long discountAmount;

	@Column(nullable = false)
	private Long finalPrice;

	@Column(nullable = false)
	private LocalDateTime usedAt;

	@Builder
	public CouponUseHistory(CouponIssue couponIssue,
									Product product,
									Long originalPrice,
									Long discountAmount,
									Long finalPrice,
									LocalDateTime usedAt
	) {
		this.couponIssue = couponIssue;
		this.product = product;
		this.originalPrice = originalPrice;
		this.discountAmount = discountAmount;
		this.finalPrice = finalPrice;
		this.usedAt = usedAt;
	}

}
