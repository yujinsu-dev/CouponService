package com.dev.coupon.coupon.service;

import com.dev.coupon.coupon.repository.CouponEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CouponStockResyncService {

	private final CouponEventRepository eventRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markPending(Long couponEventId) {
		int updated = eventRepository.markStockResyncPending(couponEventId);

		if (updated == 0) {
			log.warn("[STOCK_RESYNC_PENDING_MARK_SKIPPED] eventId={}", couponEventId);
		}
	}
}
