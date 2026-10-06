package com.bigobooks.specifications;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.entities.orders.OrderStatus;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

/**
 * Filtros paginados de ventas.
 */
public final class OrderSpecifications {

	private OrderSpecifications() {
	}

	public static Specification<Order> build(OrderStatus status, Long warehouseId, String couponCode, Long bookId,
			LocalDateTime createdFrom, LocalDateTime createdTo) {
		return (Root<Order> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}
			if (warehouseId != null) {
				predicates.add(cb.equal(root.get("warehouseId"), warehouseId));
			}
			if (couponCode != null && !couponCode.isBlank()) {
				Join<Object, Object> coupon = root.join("coupon", JoinType.LEFT);
				predicates.add(cb.like(cb.lower(coupon.get("code")), "%" + couponCode.trim().toLowerCase() + "%"));
			}
			if (bookId != null) {
				// Subconsulta para no duplicar filas al paginar.
				Subquery<Long> subquery = query.subquery(Long.class);
				Root<OrderDetail> detail = subquery.from(OrderDetail.class);
				subquery.select(detail.get("id"))
						.where(cb.equal(detail.get("order"), root), cb.equal(detail.get("bookId"), bookId));
				predicates.add(cb.exists(subquery));
			}
			if (createdFrom != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
			}
			if (createdTo != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
