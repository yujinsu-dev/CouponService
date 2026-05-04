package com.dev.coupon.coupon.service;

import com.dev.coupon.common.util.CouponRedisKey;
import com.dev.coupon.coupon.domain.CouponEvent;
import com.dev.coupon.coupon.domain.CouponIssue;
import com.dev.coupon.coupon.domain.DiscountType;
import com.dev.coupon.coupon.domain.EventStatus;
import com.dev.coupon.coupon.domain.IssueStatus;
import com.dev.coupon.coupon.repository.CouponEventRepository;
import com.dev.coupon.coupon.repository.CouponIssueRepository;
import com.dev.coupon.user.domain.User;
import com.dev.coupon.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@Rollback(value = false)
@ActiveProfiles("test")
class CouponIssueServiceTest {

	@Autowired
	private CouponIssueService issueService;

	@Autowired
	private CouponEventRepository eventRepository;

	@Autowired
	private CouponIssueRepository issueRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RedisIssueService redisIssueService;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Test
	@DisplayName("정상 발급 테스트, Redis 선점 성공 후 쿠폰 발급 시 재고가 차감, 발급 이력 저장")
	void issueCoupon() {
		User user = userRepository.save(User.builder()
				  .name("test user " + UUID.randomUUID())
				  .build());
		CouponEvent event = saveIssuableEvent();
		initRedisIssueState(event);

		Long issueId = issueService.issueCoupon(event.getId(), user.getId());

		CouponIssue couponIssue = issueRepository.findById(issueId).orElseThrow();
		CouponEvent savedEvent = eventRepository.findById(event.getId()).orElseThrow();

		assertThat(savedEvent.getRemainingQuantity()).isEqualTo(99);
		assertThat(couponIssue.getCouponEvent().getId()).isEqualTo(event.getId());
		assertThat(couponIssue.getUser().getId()).isEqualTo(user.getId());
		assertThat(couponIssue.getStatus()).isEqualTo(IssueStatus.ISSUED);
		assertThat(couponIssue.getIssuedAt()).isNotNull();
		assertThat(redisTemplate.opsForValue().get(CouponRedisKey.stock(event.getId()))).isEqualTo("99");
		assertThat(redisTemplate.opsForSet().isMember(
				  CouponRedisKey.issuedUsers(event.getId()),
				  user.getId().toString()
		)).isTrue();
	}

	private CouponEvent saveIssuableEvent() {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime validIssueStartAt = now.plusDays(1);
		LocalDateTime validIssueEndAt = validIssueStartAt.plusDays(1);

		CouponEvent event = CouponEvent.create(
				  "test coupon " + UUID.randomUUID(),
				  EventStatus.OPEN,
				  DiscountType.FIXED_AMOUNT,
				  1000L,
				  null,
				  100,
				  validIssueStartAt,
				  validIssueEndAt
		);

		ReflectionTestUtils.setField(event, "issueStartAt", now.minusMinutes(1));
		ReflectionTestUtils.setField(event, "issueEndAt", now.plusDays(1));

		return eventRepository.saveAndFlush(event);
	}

	private void initRedisIssueState(CouponEvent event) {
		redisIssueService.initEventIssueState(
				  event.getId(),
				  event.getRemainingQuantity(),
				  event.getIssueStartAt(),
				  event.getIssueEndAt()
		);
	}
}
