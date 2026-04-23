package com.dev.coupon.coupon.repository;

import com.dev.coupon.coupon.domain.CouponIssue;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select ci from CouponIssue ci where ci.id = :id and ci.user.id = :userId")
	Optional<CouponIssue> findByIdAndUserIdForUpdate(
			  @Param("id") Long id,
			  @Param("userId") Long userId);
}
