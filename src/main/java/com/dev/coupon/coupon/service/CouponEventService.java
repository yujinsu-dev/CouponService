package com.dev.coupon.coupon.service;

import com.dev.coupon.common.exception.BusinessException;
import com.dev.coupon.coupon.domain.CouponEvent;
import com.dev.coupon.coupon.domain.DiscountType;
import com.dev.coupon.coupon.dto.CouponEventCreateRequest;
import com.dev.coupon.coupon.dto.CouponEventResponse;
import com.dev.coupon.coupon.exception.CouponErrorCode;
import com.dev.coupon.coupon.repository.CouponEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponEventService {

	private final CouponEventRepository repository;

	@Transactional
	public CouponEventResponse create(CouponEventCreateRequest request) {

		CouponEvent event = repository.save(CouponEvent.create(
				  request.getName(),
				  request.getStatus(),
				  request.getDiscountType(),
				  request.getDiscountValue(),
				  request.getMaxDiscountAmount(),
				  request.getTotalQuantity(),
				  request.getIssueStartAt(),
				  request.getIssueEndAt()
		));

		return new CouponEventResponse(
				  event.getId(),
				  event.getName(),
				  event.getStatus(),
				  event.getDiscountType(),
				  event.getDiscountValue(),
				  event.getMaxDiscountAmount(),
				  event.getTotalQuantity(),
				  event.getIssueStartAt(),
				  event.getIssueEndAt()
		);
	}

	public List<CouponEventResponse> getCouponEvents() {
		return repository.findAll().stream().map(event ->
				  new CouponEventResponse(
							 event.getId(),
							 event.getName(),
							 event.getStatus(),
							 event.getDiscountType(),
							 event.getDiscountValue(),
							 event.getMaxDiscountAmount(),
							 event.getTotalQuantity(),
							 event.getIssueStartAt(),
							 event.getIssueEndAt()
				  )).toList();
	}


}
