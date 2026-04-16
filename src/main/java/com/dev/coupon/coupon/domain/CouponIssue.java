package com.dev.coupon.coupon.domain;

import com.dev.coupon.common.BaseEntity;
import com.dev.coupon.user.domain.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
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

	private LocalDateTime used_at;
	private LocalDateTime expired_at;


}
