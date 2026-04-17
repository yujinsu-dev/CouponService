package com.dev.coupon.product.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class PageResponse<T> {
	private final List<T> data;
	private final int page;
	private final int size;
	private final long totalElements;
	private final int totalPages;
	private final boolean hasNext;

	public PageResponse(List<T> data, int page, int size, long totalElements, int totlaPages, boolean hasNext) {
		this.data = data;
		this.page = page;
		this.size = size;
		this.totalElements = totalElements;
		this.totalPages = totlaPages;
		this.hasNext = hasNext;
	}
}
