package com.dev.coupon.coupon.repository;

import com.dev.coupon.coupon.domain.CouponIssue;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {
	Optional<CouponIssue> findByIdAndUserId(Long id, Long userId);
}
