package com.dev.coupon.coupon.service;

import com.dev.coupon.common.exception.BusinessException;
import com.dev.coupon.coupon.domain.CouponEvent;
import com.dev.coupon.coupon.domain.CouponIssue;
import com.dev.coupon.coupon.domain.CouponUseHistory;
import com.dev.coupon.coupon.domain.DiscountType;
import com.dev.coupon.coupon.domain.EventStatus;
import com.dev.coupon.coupon.domain.IssueStatus;
import com.dev.coupon.coupon.exception.CouponErrorCode;
import com.dev.coupon.coupon.repository.CouponEventRepository;
import com.dev.coupon.coupon.repository.CouponIssueRepository;
import com.dev.coupon.product.domain.Product;
import com.dev.coupon.product.repository.ProductRepository;
import com.dev.coupon.user.domain.User;
import com.dev.coupon.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CouponUesServiceTest {

	@Autowired
	private CouponUseService useService;

	@Autowired
	private CouponEventRepository eventRepository;

	@Autowired
	private CouponIssueRepository issueRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private UserRepository userRepository;

	@PersistenceContext
	private EntityManager em;

	@Test
	@DisplayName("쿠폰 사용 성공")
	void useCoupon() {
		UseCouponFixture fixture = saveUseCouponFixture(IssueStatus.ISSUED, LocalDateTime.now().plusDays(1), null);

		useService.useCoupon(fixture.issue().getId(), fixture.user().getId(), fixture.product().getId());

		CouponIssue usedIssue = issueRepository.findById(fixture.issue().getId()).orElseThrow();
		CouponUseHistory history = findUseHistory(fixture.issue().getId());

		assertThat(usedIssue.getStatus()).isEqualTo(IssueStatus.USED);
		assertThat(usedIssue.getUsedAt()).isNotNull();
		assertThat(history.getOriginalPrice()).isEqualTo(10000L);
		assertThat(history.getDiscountAmount()).isEqualTo(1000L);
		assertThat(history.getFinalPrice()).isEqualTo(9000L);
		assertThat(history.getUsedAt()).isNotNull();
	}

	@Test
	@DisplayName("만료 쿠폰 사용 실패")
	void useExpiredCoupon() {
		UseCouponFixture fixture = saveUseCouponFixture(IssueStatus.ISSUED, LocalDateTime.now().minusDays(1), null);

		assertBusinessException(
				  () -> useService.useCoupon(fixture.issue().getId(), fixture.user().getId(), fixture.product().getId()),
				  CouponErrorCode.COUPON_EXPIRED
		);

		CouponIssue expiredIssue = issueRepository.findById(fixture.issue().getId()).orElseThrow();

		assertThat(expiredIssue.getStatus()).isEqualTo(IssueStatus.EXPIRED);
		assertThat(expiredIssue.getUsedAt()).isNull();
		assertThat(countUseHistory(fixture.issue().getId())).isZero();
	}

	@Test
	@DisplayName("사용 완료 쿠폰 재사용 실패")
	void useAlreadyUsedCoupon() {
		UseCouponFixture fixture = saveUseCouponFixture(
				  IssueStatus.USED,
				  LocalDateTime.now().plusDays(1),
				  LocalDateTime.now().minusMinutes(10)
		);

		assertBusinessException(
				  () -> useService.useCoupon(fixture.issue().getId(), fixture.user().getId(), fixture.product().getId()),
				  CouponErrorCode.COUPON_ALREADY_USED
		);

		assertThat(countUseHistory(fixture.issue().getId())).isZero();
	}

	@Test
	@DisplayName("사용 불가 상태 쿠폰 사용 실패")
	void useNotUsableCoupon() {
		UseCouponFixture fixture = saveUseCouponFixture(IssueStatus.EXPIRED, LocalDateTime.now().plusDays(1), null);

		assertBusinessException(
				  () -> useService.useCoupon(fixture.issue().getId(), fixture.user().getId(), fixture.product().getId()),
				  CouponErrorCode.COUPON_NOT_USABLE
		);

		assertThat(countUseHistory(fixture.issue().getId())).isZero();
	}

	private UseCouponFixture saveUseCouponFixture(
			  IssueStatus issueStatus,
			  LocalDateTime issueEndAt,
			  LocalDateTime usedAt
	) {
		User user = userRepository.save(User.builder()
				  .name("test user " + UUID.randomUUID())
				  .build());

		Product product = productRepository.save(Product.builder()
				  .name("test product " + UUID.randomUUID())
				  .price(10000L)
				  .build());

		CouponEvent event = saveCouponEvent(issueEndAt);
		CouponIssue issue = issueRepository.save(new CouponIssue(
				  event,
				  user,
				  issueStatus,
				  LocalDateTime.now().minusHours(1),
				  usedAt
		));

		return new UseCouponFixture(user, product, issue);
	}

	private CouponEvent saveCouponEvent(LocalDateTime issueEndAt) {
		LocalDateTime validIssueStartAt = LocalDateTime.now().plusDays(1);
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

		ReflectionTestUtils.setField(event, "issueStartAt", LocalDateTime.now().minusDays(1));
		ReflectionTestUtils.setField(event, "issueEndAt", issueEndAt);

		return eventRepository.save(event);
	}

	private CouponUseHistory findUseHistory(Long issueId) {
		return em.createQuery(
							 "select h from CouponUseHistory h where h.couponIssue.id = :issueId",
							 CouponUseHistory.class
				  )
				  .setParameter("issueId", issueId)
				  .getSingleResult();
	}

	private long countUseHistory(Long issueId) {
		return em.createQuery(
							 "select count(h) from CouponUseHistory h where h.couponIssue.id = :issueId",
							 Long.class
				  )
				  .setParameter("issueId", issueId)
				  .getSingleResult();
	}

	private void assertBusinessException(Runnable runnable, CouponErrorCode errorCode) {
		assertThatThrownBy(runnable::run)
				  .isInstanceOf(BusinessException.class)
				  .satisfies(exception -> {
					  BusinessException businessException = (BusinessException) exception;
					  assertThat(businessException.getErrorCode()).isEqualTo(errorCode);
				  });
	}

	private record UseCouponFixture(User user, Product product, CouponIssue issue) {
	}
}
