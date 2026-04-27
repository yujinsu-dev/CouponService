package com.dev.coupon.coupon.repository.impl;

import com.dev.coupon.coupon.domain.IssueStatus;
import com.dev.coupon.coupon.domain.QCouponEvent;
import com.dev.coupon.coupon.domain.QCouponIssue;
import com.dev.coupon.coupon.repository.CouponIssueQueryRepository;
import com.dev.coupon.coupon.dto.UserCouponResponse;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CouponIssueQueryRepositoryImpl implements CouponIssueQueryRepository {

	private final JPAQueryFactory queryFactory;

	@Override
	public Page<UserCouponResponse> findUsableCouponsByUserId(Long userId, Pageable pageable) {
		QCouponEvent couponEvent = QCouponEvent.couponEvent;
		QCouponIssue couponIssue = QCouponIssue.couponIssue;

		List<UserCouponResponse> content = queryFactory
				  .select(Projections.constructor(
							 UserCouponResponse.class,
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
				  .where(
							 couponIssue.user.id.eq(userId),
							 usableCoponCondition(couponEvent, couponIssue)
				  )
				  .orderBy(couponIssue.id.desc())
				  .offset(pageable.getOffset())
				  .limit(pageable.getPageSize())
				  .fetch();

		JPAQuery<Long> countQuery = queryFactory
				  .select(couponIssue.count())
				  .from(couponIssue)
				  .where(
							 couponIssue.user.id.eq(userId),
							 usableCoponCondition(couponEvent, couponIssue)
				  );

		return PageableExecutionUtils.getPage(content, pageable, () -> {
			Long count = countQuery.fetchOne();
			return count == null ? 0L : count;
		});
	}

	public BooleanExpression usableCoponCondition(QCouponEvent event, QCouponIssue issue) {
		LocalDateTime now = LocalDateTime.now();
		return issue.status.eq(IssueStatus.ISSUED)
				  .and(event.issueStartAt.loe(now))
				  .and(event.issueEndAt.gt(now));
	}
}
