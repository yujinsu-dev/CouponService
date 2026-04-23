package com.dev.coupon.coupon.service;

import com.dev.coupon.common.exception.BusinessException;
import com.dev.coupon.coupon.domain.CouponIssue;
import com.dev.coupon.coupon.domain.CouponUseHistory;
import com.dev.coupon.coupon.exception.CouponErrorCode;
import com.dev.coupon.coupon.exception.ExpiredCouponException;
import com.dev.coupon.coupon.repository.CouponIssueRepository;
import com.dev.coupon.coupon.repository.CouponUseHistoryRepository;
import com.dev.coupon.product.domain.Product;
import com.dev.coupon.product.exeception.ProductErrorCode;
import com.dev.coupon.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponUseService {

	private final CouponIssueRepository issueRepository;
	private final ProductRepository productRepository;
	private final CouponUseHistoryRepository historyRepository;

	@Transactional(noRollbackFor = ExpiredCouponException.class)
	public void useCoupon(Long issueId, Long userId, Long productId) {
		LocalDateTime usedAt = LocalDateTime.now();

		CouponIssue couponIssue = issueRepository.findByIdAndUserId(issueId, userId)
				  .orElseThrow(() -> new BusinessException(CouponErrorCode.COUPON_ISSUE_NOT_FOUND));

		Product product = productRepository.findById(productId)
				  .orElseThrow(() -> new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND));

		couponIssue.use(usedAt);

		Long productPrice = product.getPrice();
		Long discountAmount = couponIssue.getCouponEvent().calculateDiscountAmount(productPrice);
		long finalPrice = productPrice - discountAmount;

		historyRepository.save(CouponUseHistory.builder()
				  .couponIssue(couponIssue)
				  .product(product)
				  .originalPrice(productPrice)
				  .discountAmount(discountAmount)
				  .finalPrice(finalPrice)
				  .usedAt(usedAt)
				  .build());
	}
}
