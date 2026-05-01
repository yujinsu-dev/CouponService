package com.dev.coupon.coupon.repository;

import com.dev.coupon.coupon.domain.CouponIssue;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select ci from CouponIssue ci where ci.id = :id and ci.user.id = :userId")
	Optional<CouponIssue> findByIdAndUserIdForUpdate(
			  @Param("id") Long id,
			  @Param("userId") Long userId
	);

	int countByCouponEventId(Long eventId);

	@Query("select ci.user.id from CouponIssue ci where ci.couponEvent.id = :eventId")
	List<Long> findUserIdByCouponEventId(@Param("eventId") Long eventId);
}
