package com.dev.coupon.coupon.repository;

import com.dev.coupon.coupon.domain.CouponUseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponUseHistoryRepository extends JpaRepository<CouponUseHistory, Long> {
}
