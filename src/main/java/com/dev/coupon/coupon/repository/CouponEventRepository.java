package com.dev.coupon.coupon.repository;

import com.dev.coupon.coupon.domain.CouponEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponEventRepository extends JpaRepository<CouponEvent, Long>, CouponEventQueryRepository {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			  	update CouponEvent ce
			  	set ce.remainingQuantity = ce.remainingQuantity -1
			  	where ce.id = :couponEventId
			  	and ce.remainingQuantity > 0
			""")
	void decreaseStockIfAvailable(@Param("couponEventId") Long couponEventId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
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
					ce.stockResyncPending = false
				where ce.id = :couponEventId
			""")
	int completeStockResync(
			  @Param("couponEventId") Long couponEventId,
			  @Param("remainingQuantity") int remainingQuantity
	);

	@Query("select ce.totalQuantity from CouponEvent ce where ce.id = :eventId")
	int findTotalQuantityById(@Param("eventId") Long eventId);
}
