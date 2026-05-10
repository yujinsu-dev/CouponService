package com.dev.coupon.product.service;

import com.dev.coupon.common.exception.BusinessException;
import com.dev.coupon.product.domain.Product;
import com.dev.coupon.common.PageResponse;
import com.dev.coupon.product.dto.ProductCreateRequest;
import com.dev.coupon.product.dto.ProductResponse;
import com.dev.coupon.product.dto.ProductCondition;
import com.dev.coupon.product.exception.ProductErrorCode;
import com.dev.coupon.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

	private final ProductRepository repository;

	@Transactional
	public ProductResponse create(ProductCreateRequest request) {
		Product product = repository.save(Product.builder()
				  .name(request.getName())
				  .price(request.getPrice())
				  .build());
		return ProductResponse.from(product);
	}

	/*
	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> getProductPage(Pageable pageable) {
		Page<ProductResponse> page = repository.findAll(pageable)
				  .map(ProductResponse::from);

		return PageResponse.from(page);
	}
	*/

	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> search(ProductCondition condition, Pageable pageable) {
		validationSearchCondition(condition);
		Page<ProductResponse> page = repository.search(condition, pageable);

		return PageResponse.from(page);
	}

	private void validationSearchCondition(ProductCondition condition) {
		if (condition.getMinPrice() != null
				  && condition.getMaxPrice() != null
				  && condition.getMinPrice() > condition.getMaxPrice()) {
			throw new BusinessException(ProductErrorCode.INVALID_PRODUCT_SEARCH_CONDITION);
		}
	}
}
