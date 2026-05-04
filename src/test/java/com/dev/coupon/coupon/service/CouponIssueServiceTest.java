package com.dev.coupon.coupon.service;

import com.dev.coupon.common.exception.BusinessException;
import com.dev.coupon.common.util.CouponRedisKey;
import com.dev.coupon.coupon.domain.CouponEvent;
import com.dev.coupon.coupon.domain.CouponIssue;
import com.dev.coupon.coupon.domain.DiscountType;
import com.dev.coupon.coupon.domain.EventStatus;
import com.dev.coupon.coupon.domain.IssueStatus;
import com.dev.coupon.coupon.exception.CouponErrorCode;
import com.dev.coupon.coupon.repository.CouponEventRepository;
import com.dev.coupon.coupon.repository.CouponIssueRepository;
import com.dev.coupon.user.domain.User;
import com.dev.coupon.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
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
		User user = saveUser();
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
		assertThat(redisStock(event)).isEqualTo("99");
		assertThat(redisIssuedUserContains(event, user)).isTrue();
	}

	@Test
	@DisplayName("Redis에서 발급된 사용자면 중복 발급 예외 발생")
	void issueCouponAlreadyIssued() {
		User user = saveUser();
		CouponEvent event = saveIssuableEvent();
		initRedisIssueState(event);
		redisTemplate.opsForSet().add(CouponRedisKey.issuedUsers(event.getId()), user.getId().toString());

		assertBusinessException(
				  () -> issueService.issueCoupon(event.getId(), user.getId()),
				  CouponErrorCode.COUPON_ALREADY_ISSUE
		);

		assertIssueNotCreated(event);
		assertThat(eventRemainingQuantity(event)).isEqualTo(100);
		assertThat(redisStock(event)).isEqualTo("100");
		assertThat(redisIssuedUserContains(event, user)).isTrue();
	}

	@Test
	@DisplayName("Redis 재고가 없으면 품절 예외 발생")
	void issueCouponSoldOut() {
		User user = saveUser();
		CouponEvent event = saveIssuableEvent();
		initRedisIssueState(event);
		redisTemplate.opsForValue().set(CouponRedisKey.stock(event.getId()), "0");

		assertBusinessException(
				  () -> issueService.issueCoupon(event.getId(), user.getId()),
				  CouponErrorCode.COUPON_SOLD_OUT
		);

		assertIssueNotCreated(event);
		assertThat(eventRemainingQuantity(event)).isEqualTo(100);
		assertThat(redisStock(event)).isEqualTo("0");
		assertThat(redisIssuedUserContains(event, user)).isFalse();
	}

	@Test
	@DisplayName("발급 시작 전 이벤트 발급 불가")
	void issueCouponNotStarted() {
		User user = saveUser();
		LocalDateTime now = LocalDateTime.now();
		CouponEvent event = saveEvent(
				  EventStatus.OPEN,
				  now.plusDays(1),
				  now.plusDays(2),
				  false
		);
		initRedisIssueState(event);

		assertBusinessException(
				  () -> issueService.issueCoupon(event.getId(), user.getId()),
				  CouponErrorCode.COUPON_NOT_ISSUABLE
		);

		assertIssueNotCreated(event);
		assertThat(eventRemainingQuantity(event)).isEqualTo(100);
		assertThat(redisStock(event)).isEqualTo("100");
		assertThat(redisIssuedUserContains(event, user)).isFalse();
	}

	@Test
	@DisplayName("발급 종료된 이벤트 발급 불가")
	void issueCouponEnded() {
		User user = saveUser();
		LocalDateTime now = LocalDateTime.now();
		CouponEvent event = saveEvent(
				  EventStatus.OPEN,
				  now.minusDays(2),
				  now.minusDays(1),
				  false
		);
		initRedisIssueState(event);

		assertBusinessException(
				  () -> issueService.issueCoupon(event.getId(), user.getId()),
				  CouponErrorCode.COUPON_NOT_ISSUABLE
		);

		assertIssueNotCreated(event);
		assertThat(eventRemainingQuantity(event)).isEqualTo(100);
		assertThat(redisStock(event)).isEqualTo("100");
		assertThat(redisIssuedUserContains(event, user)).isFalse();
	}

	@Test
	@DisplayName("CLOSED 상태 이벤트는 Redis 선점 후 발급이 rollback되고 Redis 원복")
	void issueCouponClosedEvent() {
		User user = saveUser();
		CouponEvent event = saveEvent(
				  EventStatus.CLOSED,
				  LocalDateTime.now().minusMinutes(1),
				  LocalDateTime.now().plusDays(1),
				  false
		);
		initRedisIssueState(event);

		assertBusinessException(
				  () -> issueService.issueCoupon(event.getId(), user.getId()),
				  CouponErrorCode.COUPON_NOT_ISSUABLE
		);

		assertIssueNotCreated(event);
		assertThat(eventRemainingQuantity(event)).isEqualTo(100);
		assertThat(redisStock(event)).isEqualTo("100");
		assertThat(redisIssuedUserContains(event, user)).isFalse();
	}

	@Test
	@DisplayName("재고 재동기화 대기 이벤트는 Redis 선점 후 발급이 rollback되고 Redis 원복")
	void issueCouponStockResyncPendingEvent() {
		User user = saveUser();
		CouponEvent event = saveEvent(
				  EventStatus.OPEN,
				  LocalDateTime.now().minusMinutes(1),
				  LocalDateTime.now().plusDays(1),
				  true
		);
		initRedisIssueState(event);

		assertBusinessException(
				  () -> issueService.issueCoupon(event.getId(), user.getId()),
				  CouponErrorCode.COUPON_NOT_ISSUABLE
		);

		assertIssueNotCreated(event);
		assertThat(eventRemainingQuantity(event)).isEqualTo(100);
		assertThat(redisStock(event)).isEqualTo("100");
		assertThat(redisIssuedUserContains(event, user)).isFalse();
	}

	private User saveUser() {
		return userRepository.save(User.builder()
				  .name("test user " + UUID.randomUUID())
				  .build());
	}

	private CouponEvent saveIssuableEvent() {
		LocalDateTime now = LocalDateTime.now();
		return saveEvent(
				  EventStatus.OPEN,
				  now.minusMinutes(1),
				  now.plusDays(1),
				  false
		);
	}

	private CouponEvent saveEvent(
			  EventStatus status,
			  LocalDateTime issueStartAt,
			  LocalDateTime issueEndAt,
			  boolean stockResyncPending
	) {
		LocalDateTime validIssueStartAt = LocalDateTime.now().plusDays(1);
		LocalDateTime validIssueEndAt = validIssueStartAt.plusDays(1);

		CouponEvent event = CouponEvent.create(
				  "test coupon " + UUID.randomUUID(),
				  status,
				  DiscountType.FIXED_AMOUNT,
				  1000L,
				  null,
				  100,
				  validIssueStartAt,
				  validIssueEndAt
		);

		ReflectionTestUtils.setField(event, "issueStartAt", issueStartAt);
		ReflectionTestUtils.setField(event, "issueEndAt", issueEndAt);
		ReflectionTestUtils.setField(event, "stockResyncPending", stockResyncPending);

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

	private void assertBusinessException(Runnable runnable, CouponErrorCode errorCode) {
		assertThatThrownBy(runnable::run)
				  .isInstanceOf(BusinessException.class)
				  .satisfies(exception -> {
					  BusinessException businessException = (BusinessException) exception;
					  assertThat(businessException.getErrorCode()).isEqualTo(errorCode);
				  });
	}

	private void assertIssueNotCreated(CouponEvent event) {
		assertThat(issueRepository.countByCouponEventId(event.getId())).isZero();
	}

	private int eventRemainingQuantity(CouponEvent event) {
		return eventRepository.findById(event.getId())
				  .orElseThrow()
				  .getRemainingQuantity();
	}

	private String redisStock(CouponEvent event) {
		return redisTemplate.opsForValue().get(CouponRedisKey.stock(event.getId()));
	}

	private boolean redisIssuedUserContains(CouponEvent event, User user) {
		return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(
				  CouponRedisKey.issuedUsers(event.getId()),
				  user.getId().toString()
		));
	}
}
