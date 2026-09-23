package com.bigobooks.specifications;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bigobooks.entities.rents.Rent;
import com.bigobooks.entities.rents.RentCoupon;
import com.bigobooks.entities.rents.RentDetail;

import jakarta.persistence.criteria.Join;

import org.springframework.data.jpa.domain.Specification;

/**
 * Especificaciones de busqueda para rentas ({@code Specification} de Spring
 * Data JPA). Cada metodo agrega un filtro opcional; con {@link #build} se
 * combinan los que no vienen vacios.
 */
public final class RentSpecifications {

	private RentSpecifications() {
	}

	public static Specification<Rent> build(Long userId, Long bookId, LocalDateTime createdFrom,
			LocalDateTime createdTo, Boolean overdue, String couponCode) {
		List<Specification<Rent>> specs = new ArrayList<>();
		if (userId != null) {
			specs.add(byUserId(userId));
		}
		if (bookId != null) {
			specs.add(byBookId(bookId));
		}
		if (createdFrom != null) {
			specs.add(byCreatedFrom(createdFrom));
		}
		if (createdTo != null) {
			specs.add(byCreatedTo(createdTo));
		}
		if (Boolean.TRUE.equals(overdue)) {
			specs.add(byOverdue());
		}
		if (couponCode != null && !couponCode.isBlank()) {
			specs.add(byCouponCode(couponCode.trim()));
		}
		return Specification.allOf(specs);
	}

	public static Specification<Rent> byUserId(Long userId) {
		return (root, query, cb) -> cb.equal(root.get("userAccount").get("id"), userId);
	}

	public static Specification<Rent> byBookId(Long bookId) {
		return (root, query, cb) -> {
			Join<Rent, RentDetail> details = root.join("rentDetails");
			query.distinct(true);
			return cb.equal(details.get("book").get("id"), bookId);
		};
	}

	public static Specification<Rent> byCreatedFrom(LocalDateTime from) {
		return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
	}

	public static Specification<Rent> byCreatedTo(LocalDateTime to) {
		return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to);
	}

	public static Specification<Rent> byOverdue() {
		return (root, query, cb) -> {
			Join<Rent, RentDetail> details = root.join("rentDetails");
			query.distinct(true);
			return cb.lessThan(details.get("returnDate"), LocalDate.now());
		};
	}

	public static Specification<Rent> byCouponCode(String couponCode) {
		return (root, query, cb) -> {
			Join<Rent, RentCoupon> coupons = root.join("rentCoupons");
			query.distinct(true);
			return cb.equal(coupons.get("coupon").get("code"), couponCode);
		};
	}
}