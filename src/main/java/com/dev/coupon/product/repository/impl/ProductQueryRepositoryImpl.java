package com.dev.coupon.product.repository.impl;

import com.dev.coupon.product.domain.Product;
import com.dev.coupon.product.dto.ProductResponse;
import com.dev.coupon.product.repository.ProductCondition;
import com.dev.coupon.product.repository.ProductQueryRepository;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import static com.dev.coupon.product.domain.QProduct.product;
import static org.springframework.util.StringUtils.hasText;

import java.util.List;

@Repository
public class ProductQueryRepositoryImpl implements ProductQueryRepository {

	private final JPAQueryFactory queryFactory;

	public ProductQueryRepositoryImpl(EntityManager em) {
		this.queryFactory = new JPAQueryFactory(em);
	}

	@Override
	public Page<ProductResponse> search(ProductCondition condition, Pageable pageable) {
		List<ProductResponse> content = queryFactory
				  .select(Projections.constructor(
							 ProductResponse.class,
							 product.id,
							 product.name,
							 product.price
				  ))
				  .from(product)
				  .where(
							 nameContains(condition.getName()),
							 priceBetween(condition.getMinPrice(), condition.getMaxPrice())
				  )
				  .orderBy(product.id.desc())
				  .offset(pageable.getOffset())
				  .limit(pageable.getPageSize())
				  .fetch();

		JPAQuery<Long> countQuery = queryFactory
				  .select(product.count())
				  .from(product)
				  .where(
							 nameContains(condition.getName()),
							 priceBetween(condition.getMinPrice(), condition.getMaxPrice())
				  );

		return PageableExecutionUtils.getPage(content, pageable, () -> {
			Long count = countQuery.fetchOne();
			return count == null ? 0L : count;
		});
	}

	private BooleanExpression nameContains(String name) {
		return hasText(name) ? product.name.contains(name) : null;
	}

	private BooleanExpression priceBetween(Long minPrice, Long maxPrice) {
		if (minPrice != null && maxPrice != null) {
			return product.price.between(minPrice, maxPrice);
		}

		if (minPrice != null) {
			return product.price.goe(minPrice);
		}

		if (maxPrice != null) {
			return product.price.loe(maxPrice);
		}

		return null;
	}
}
