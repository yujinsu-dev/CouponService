package com.dev.coupon.coupon.repository;

import com.dev.coupon.coupon.domain.CouponEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface CouponEventRepository extends JpaRepository<CouponEvent, Long>, CouponEventQueryRepository {

	@Modifying(clearAutomatically = true)
	@Query("""
			  	update CouponEvent ce
			  	set ce.remainingQuantity = ce.remainingQuantity -1
			  	where ce.id = :couponEventId
			  	and ce.remainingQuantity > 0
			""")
	int decreaseStockIfAvailable(@Param("couponEventId") Long couponEventId);

	@Modifying
	@Query("""
				update CouponEvent ce
				set ce.stockResyncPending = true
				where ce.id = :couponEventId
			""")
	int markStockResyncPending(@Param("couponEventId") Long couponEventId);

	@Modifying(clearAutomatically = true)
	@Query("""
			  	update CouponEvent ce
			  	set ce.remainingQuantity = :remainingQuantity,
			  		ce.stockResyncPending = false,
			  		ce.issueStartAt = :issueStartAt,
			  		ce.issueEndAt = :issueEndAt
			  	where ce.id = :couponEventId
			  """)
	int completeStockResync(
			  @Param("couponEventId") Long couponEventId,
			  @Param("remainingQuantity") int remainingQuantity,
			  @Param("issueStartAt") LocalDateTime issueStartAt,
			  @Param("issueEndAt") LocalDateTime issueEndAt
	);

	@Modifying(clearAutomatically = true)
	@Query("""
				update CouponEvent ce
				set ce.status = 'CLOSED'
				where ce.status = 'OPEN'
				and ce.issueEndAt <= :now
			""")
	int closeExpiredEvents(LocalDateTime now);

}
