package com.dev.coupon.coupon.service;

import com.dev.coupon.common.exception.BusinessException;
import com.dev.coupon.coupon.domain.CouponEvent;
import com.dev.coupon.coupon.dto.CouponEventCreateRequest;
import com.dev.coupon.coupon.dto.CouponEventResponse;
import com.dev.coupon.coupon.dto.condition.CouponEventCondition;
import com.dev.coupon.coupon.exception.CouponErrorCode;
import com.dev.coupon.coupon.repository.CouponEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CouponEventService {

	private final CouponEventRepository repository;
	private final RedisIssueService redisIssueService;
	private final CouponStockResyncService resyncService;

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

		// 쿠폰 수량 init
		initEventStockAfterCommit(
				  event.getId(),
				  event.getRemainingQuantity(),
				  event.getIssueStartAt(),
				  event.getIssueEndAt()
		);

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

	/* 목록 호출
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
	*/

	private void initEventStockAfterCommit(
			  Long eventId,
			  int remainingQuantity,
			  LocalDateTime issueStartAt,
			  LocalDateTime issueEndAt
	) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				initEventStockOrMarkPending(eventId, remainingQuantity, issueStartAt, issueEndAt);
			}
		});
	}

	private void initEventStockOrMarkPending(
			  Long eventId,
			  int remainingQuantity,
			  LocalDateTime issueStartAt,
			  LocalDateTime issueEndAt
	) {
		try {
			// Redis 초기화가 실패할 가능성이 있기 때문에 Exception 발생 시 마킹
			redisIssueService.initEventIssueState(
					  eventId,
					  remainingQuantity,
					  issueStartAt,
					  issueEndAt
			);
		} catch (Exception e) {
			log.error("[REDIS_STOCK_INIT_FAILED] eventId = {}, remainingQuantity = {}", eventId, remainingQuantity, e);
			resyncService.markPending(eventId);
		}
	}

	public List<CouponEventResponse> search(CouponEventCondition condition) {
		if (condition.getSearchStartAt() != null
				&& condition.getSearchEndAt() != null
				&& condition.getSearchEndAt().isBefore(condition.getSearchStartAt())) {
			throw new BusinessException(CouponErrorCode.INVALID_COUPON_EVENT_SEARCH_CONDITION);
		}

		return repository.search(condition);
	}


}
