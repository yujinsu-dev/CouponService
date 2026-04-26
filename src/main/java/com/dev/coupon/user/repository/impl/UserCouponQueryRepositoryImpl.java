package com.dev.coupon.user.repository.impl;

import com.dev.coupon.coupon.domain.QCouponEvent;
import com.dev.coupon.coupon.domain.QCouponIssue;
import com.dev.coupon.user.dto.MyCouponListResponse;
import com.dev.coupon.user.repository.UserCouponQueryRepository;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;


@RequiredArgsConstructor
public class UserCouponQueryRepositoryImpl implements UserCouponQueryRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<MyCouponListResponse> getMyCoupons(Long userId, Pageable pageable) {
		QCouponEvent couponEvent = QCouponEvent.couponEvent;
		QCouponIssue couponIssue = QCouponIssue.couponIssue;

		List<MyCouponListResponse> content = queryFactory
				  .select(Projections.constructor(
							 MyCouponListResponse.class,
							 couponEvent.id,
							 couponIssue.id,
							 couponEvent.name,
							 couponIssue.status,
							 couponEvent.discountType,
							 couponEvent.discountValue,
							 couponEvent.maxDiscountAmount,
							 couponEvent.issueEndAt))
				  .from(couponIssue)
				  .join(couponIssue.couponEvent, couponEvent)
				  .where(couponIssue.user.id.eq(userId))
				  .orderBy(couponIssue.id.desc())
				  .offset(pageable.getOffset())
				  .limit(pageable.getPageSize())
				  .fetch();

		JPAQuery<Long> countQuery = queryFactory
				  .select(couponIssue.count())
				  .from(couponIssue)
				  .where(couponIssue.user.id.eq(userId));

		return PageableExecutionUtils.getPage(content, pageable, () -> {
			Long count = countQuery.fetchOne();
			return count == null ? 0L : count;
		});
	}
}
