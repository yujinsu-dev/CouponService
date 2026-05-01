package com.dev.coupon.coupon.service;

import com.dev.coupon.common.exception.BusinessException;
import com.dev.coupon.coupon.domain.CouponEvent;
import com.dev.coupon.coupon.exception.CouponErrorCode;
import com.dev.coupon.coupon.repository.CouponEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CouponStockRecoveryService {

	private final CouponEventRepository eventRepository;
	private final CouponStockRestoreService stockRestoreService;

	public void resync(Long eventId) {

		CouponEvent couponEvent = eventRepository.findById(eventId).
				  orElseThrow(() -> new BusinessException(CouponErrorCode.COUPON_EVENT_NOT_FOUND));

		if (couponEvent.isStockResyncPending()) {
			stockRestoreService.restoreFromDatabase(eventId);
		}


	}
}
