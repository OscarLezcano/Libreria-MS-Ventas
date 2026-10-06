package com.bigobooks.services;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Construye el Pageable de los listados paginados (pagina >= 0, size >= 1).
 */
final class Pageables {

	private Pageables() {
	}

	static Pageable of(Integer page, Integer size, String sort) {
		Sort sortSpec = Sort.unsorted();
		if (sort != null && !sort.isBlank()) {
			String[] parts = sort.split(",");
			String field = parts[0].trim();
			Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
					? Sort.Direction.DESC
					: Sort.Direction.ASC;
			sortSpec = Sort.by(direction, field);
		}
		int pageNumber = page == null ? 0 : Math.max(page, 0);
		int pageSize = size == null ? 20 : Math.max(size, 1);
		return PageRequest.of(pageNumber, pageSize, sortSpec);
	}
}
