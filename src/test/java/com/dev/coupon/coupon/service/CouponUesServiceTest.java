package com.dev.coupon.coupon.service;

import com.dev.coupon.common.exception.BusinessException;
import com.dev.coupon.coupon.domain.CouponIssue;
import com.dev.coupon.coupon.domain.IssueStatus;
import com.dev.coupon.coupon.exception.CouponErrorCode;
import com.dev.coupon.coupon.repository.CouponEventRepository;
import com.dev.coupon.coupon.repository.CouponIssueRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Slf4j
@ActiveProfiles("test")
class CouponUesServiceTest {

	@Autowired
	private EntityManager em;

	@Autowired
	private CouponIssueRepository issueRepository;

	@Test
	void couponUse() {
		LocalDateTime now = LocalDateTime.now();

		CouponIssue couponIssue = issueRepository.findById(3L).orElseThrow();
		couponIssue.use(now);

		assertThat(couponIssue.getStatus()).isEqualTo(IssueStatus.USED);
		assertThat(couponIssue.getUsedAt()).isEqualTo(now);
	}

	@Test
	void expired() {
		LocalDateTime now = LocalDateTime.now();

		CouponIssue couponIssue = issueRepository.findById(2L).orElseThrow();

		assertThatThrownBy(() -> couponIssue.use(now))
				  .isInstanceOf(BusinessException.class)
				  .satisfies(exception -> {
					  BusinessException businessException = (BusinessException) exception;
					  assertThat(businessException.getErrorCode())
								 .isEqualTo(CouponErrorCode.COUPON_EXPIRED);
				  });

		assertThat(couponIssue.getStatus()).isEqualTo(IssueStatus.EXPIRED);
	}

	@Test
	void notUsable() {
		LocalDateTime now = LocalDateTime.now();

		CouponIssue couponIssue = issueRepository.findById(1L).orElseThrow();

		assertThatThrownBy(() -> couponIssue.use(now))
				  .isInstanceOf(BusinessException.class)
				  .satisfies(exception -> {
					  BusinessException businessException = (BusinessException) exception;
					  assertThat(businessException.getErrorCode())
								 .isEqualTo(CouponErrorCode.COUPON_NOT_USABLE);
				  });

		assertThat(couponIssue.getStatus()).isNotEqualTo(IssueStatus.ISSUED);
	}

}