package com.dev.coupon.user.service;

import com.dev.coupon.common.PageResponse;
import com.dev.coupon.common.exception.BusinessException;
import com.dev.coupon.user.dto.CouponPageRequest;
import com.dev.coupon.user.dto.MyCouponListResponse;
import com.dev.coupon.user.exception.UserErrorCode;
import com.dev.coupon.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository repository;


	@Transactional(readOnly = true)
	public PageResponse<MyCouponListResponse> getMyCoupons(Long id, CouponPageRequest request) {
		if (!repository.existsById(id)) {
			throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
		}

		PageRequest pageable = PageRequest.of(
			request.getSize() == null ? 0 : request.getPage() - 1,
			request.getSize() == null ? 10 : request.getSize(),
			Sort.by(Sort.Direction.DESC, "id")
		);

		Page<MyCouponListResponse> page = repository.getMyCoupons(id, pageable);
		return PageResponse.from(page);
	}
}
