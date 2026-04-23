package com.dev.coupon.coupon.domain;

import com.dev.coupon.common.BaseEntity;
import com.dev.coupon.common.exception.BusinessException;
import com.dev.coupon.coupon.exception.CouponErrorCode;
import com.dev.coupon.coupon.exception.ExpiredCouponException;
import com.dev.coupon.user.domain.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "coupon_issue")
public class CouponIssue extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "coupon_event_id", nullable = false)
	private CouponEvent couponEvent;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private IssueStatus status;

	@Column(nullable = false)
	private LocalDateTime issuedAt;

	private LocalDateTime usedAt;

	public void use(LocalDateTime usedAt) {
		boolean isExpired = !couponEvent.getIssueEndAt().isAfter(usedAt);

		if (status == IssueStatus.USED) {
			throw new BusinessException(CouponErrorCode.COUPON_ALREADY_USED);
		}

		if (status != IssueStatus.ISSUED) {
			throw new BusinessException(CouponErrorCode.COUPON_NOT_USABLE);
		}

		if (isExpired) {
			this.status = IssueStatus.EXPIRED;
			throw new ExpiredCouponException();
		}

		this.status = IssueStatus.USED;
		this.usedAt = usedAt;
	}
}
