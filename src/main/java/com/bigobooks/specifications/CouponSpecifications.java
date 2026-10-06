package com.bigobooks.specifications;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.bigobooks.entities.orders.Coupon;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Filtros paginados de cupones.
 */
public final class CouponSpecifications {

	private CouponSpecifications() {
	}

	public static Specification<Coupon> build(String code, Boolean active, Integer minDiscountPercent) {
		return (Root<Coupon> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (code != null && !code.isBlank()) {
				predicates.add(cb.like(cb.lower(root.get("code")), "%" + code.trim().toLowerCase() + "%"));
			}
			if (active != null) {
				LocalDateTime now = LocalDateTime.now();
				if (active) {
					predicates.add(cb.or(cb.isNull(root.get("validUntil")), cb.greaterThan(root.get("validUntil"), now)));
				} else {
					predicates.add(cb.and(cb.isNotNull(root.get("validUntil")), cb.lessThan(root.get("validUntil"), now)));
				}
			}
			if (minDiscountPercent != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("discountPercent"), minDiscountPercent));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
