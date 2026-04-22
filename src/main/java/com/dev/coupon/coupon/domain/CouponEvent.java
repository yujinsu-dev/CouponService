package com.dev.coupon.coupon.domain;

import com.dev.coupon.common.BaseEntity;
import com.dev.coupon.common.exception.BusinessException;
import com.dev.coupon.coupon.dto.CouponEventCreateRequest;
import com.dev.coupon.coupon.exception.CouponErrorCode;
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

	private CouponEvent(
			  String name,
			  EventStatus status,
			  DiscountType discountType,
			  Long discountValue,
			  Long maxDiscountAmount,
			  int totalQuantity,
			  LocalDateTime issueStartAt,
			  LocalDateTime issueEndAt
	) {
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

	public static CouponEvent create(
			  String name,
			  EventStatus status,
			  DiscountType discountType,
			  Long discountValue,
			  Long maxDiscountAmount,
			  int totalQuantity,
			  LocalDateTime issueStartAt,
			  LocalDateTime issueEndAt
	) {
		validateIssuePeriod(issueStartAt, issueEndAt);
		validateDiscountPolicy(discountType, discountValue, maxDiscountAmount);

		return new CouponEvent(
				  name,
				  status,
				  discountType,
				  discountValue,
				  maxDiscountAmount,
				  totalQuantity,
				  issueStartAt,
				  issueEndAt
		);
	}

	private static void validateIssuePeriod(LocalDateTime issueStartAt, LocalDateTime issueEndAt) {
		LocalDateTime now = LocalDateTime.now();

		// 쿠폰 시작 날짜는 현재 시간보다 이전일 수 없다
		if (!issueStartAt.isAfter(now)) {
			throw new BusinessException(CouponErrorCode.INVALID_ISSUE_START_AT);
		}

		// 쿠폰 만료 날짜는 시작 날짜 이후여야 함
		if (!issueEndAt.isAfter(issueStartAt)) {
			throw new BusinessException(CouponErrorCode.INVALID_ISSUE_END_AT);
		}
	}

	private static void validateDiscountPolicy(DiscountType discountType, Long discountValue, Long maxDiscountAmount) {
		// 고정 금액 할인은 최대 할인 금액 필요 없음 discountValue로만 설정
		if (discountType == DiscountType.FIXED_AMOUNT) {
			if (maxDiscountAmount != null) {
				throw new BusinessException(CouponErrorCode.FIXED_AMOUNT_MAX_DISCOUNT_NOT_ALLOWED);
			}
		}

		// % 할인은 최대 할인 금액이 필수고 0 보다 커야 함
		if (discountType == DiscountType.PERCENTAGE) {
			if (maxDiscountAmount == null || maxDiscountAmount <= 0) {
				throw new BusinessException(CouponErrorCode.PERCENTAGE_MAX_DISCOUNT_REQUIRED);
			}
		}

		// % 할인은 discountValue가 100보다 작아야 함
		if (discountType == DiscountType.PERCENTAGE && discountValue > 100) {
			throw new BusinessException(CouponErrorCode.INVALID_PERCENTAGE_DISCOUNT_VALUE);
		}
	}

}
