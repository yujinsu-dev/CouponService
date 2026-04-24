package com.dev.coupon.coupon.service;

import com.dev.coupon.common.exception.BusinessException;
import com.dev.coupon.coupon.domain.*;
import com.dev.coupon.coupon.exception.CouponErrorCode;
import com.dev.coupon.coupon.repository.CouponEventRepository;
import com.dev.coupon.coupon.repository.CouponIssueRepository;
import com.dev.coupon.coupon.repository.CouponUseHistoryRepository;
import com.dev.coupon.product.domain.Product;
import com.dev.coupon.product.repository.ProductRepository;
import com.dev.coupon.user.domain.User;
import com.dev.coupon.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
class CouponUseServiceConcurrencyTest {

	@Autowired
	private CouponEventRepository eventRepository;

	@Autowired
	private CouponIssueRepository issueRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CouponUseHistoryRepository useHistoryRepository;

	@Autowired
	private CouponUseService couponUseService;

	@PersistenceContext
	private EntityManager em;

	private Long couponIssueId;
	private Long userId;
	private Long productId;

	@BeforeEach
	void setUp() {
		LocalDateTime now = LocalDateTime.now();

		String uuid = UUID.randomUUID().toString().substring(0, 8);
		Product product = productRepository.save(Product.builder()
				  .name("테스트 상품" + uuid)
				  .price(10000L)
				  .build());

		User user = userRepository.save(User.builder()
				  .name("테스트 유저" + uuid)
				  .build());

		CouponEvent event = eventRepository.save(CouponEvent.create(
				  "테스트 이벤트" + uuid,
				  EventStatus.OPEN,
				  DiscountType.FIXED_AMOUNT,
				  10000L,
				  null,
				  100,
				  now.plusMinutes(1),
				  now.plusDays(1)
		));
		CouponIssue couponIssue = issueRepository.save(new CouponIssue(event, user, IssueStatus.ISSUED, now, null));

		productId = product.getId();
		userId = user.getId();
		couponIssueId = couponIssue.getId();
	}

	@Test
	void concurrencyTest() throws Exception {
		int threadCount = 2;
		ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
		CountDownLatch ready = new CountDownLatch(threadCount);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger successCount = new AtomicInteger();
		AtomicInteger alreadyUsedCount = new AtomicInteger();

		List<Future<?>> futures = new ArrayList<>();

		for (int i = 0; i < threadCount; i++) {
			futures.add(executorService.submit(() -> {
				ready.countDown();
				start.await();

				try {
					couponUseService.useCoupon(couponIssueId, userId, productId);
					successCount.incrementAndGet();
					log.info("count : {} ", successCount);
				} catch (BusinessException exception) {
					if (exception.getErrorCode() == CouponErrorCode.COUPON_ALREADY_USED) {
						alreadyUsedCount.incrementAndGet();
						return null;
					}
					throw exception;
				}
				return null;
			}));
		}

		assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
		start.countDown();

		for (Future<?> future : futures) {
			future.get(10, TimeUnit.SECONDS);
		}

		executorService.shutdown();

		assertThat(successCount.get()).isEqualTo(1);
		assertThat(alreadyUsedCount.get()).isEqualTo(1);
		assertThat(issueRepository.findById(couponIssueId).orElseThrow().getStatus()).isEqualTo(IssueStatus.USED);
		assertThat(countHistory(couponIssueId)).isEqualTo(1);
	}

	private long countHistory(Long issueId) {
		return em.createQuery(
							 "select count(h) from CouponUseHistory h where h.couponIssue.id = :issueId",
							 Long.class
				  )
				  .setParameter("issueId", issueId)
				  .getSingleResult();
	}
}