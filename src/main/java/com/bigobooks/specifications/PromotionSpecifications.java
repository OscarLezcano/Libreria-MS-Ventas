package com.bigobooks.specifications;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.bigobooks.entities.book.Book;
import com.bigobooks.model.Promotion;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Filtros paginados de promociones.
 */
public final class PromotionSpecifications {

	private PromotionSpecifications() {
	}

	public static Specification<Promotion> build(String name, Boolean active, Integer minDiscountPercent, Long bookId) {
		return (Root<Promotion> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (name != null && !name.isBlank()) {
				predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%"));
			}
			if (active != null) {
				LocalDate today = LocalDate.now();
				if (active) {
					predicates.add(cb.and(cb.lessThanOrEqualTo(root.get("startDate"), today),
							cb.greaterThanOrEqualTo(root.get("endDate"), today)));
				} else {
					predicates.add(cb.or(cb.greaterThan(root.get("startDate"), today),
							cb.lessThan(root.get("endDate"), today)));
				}
			}
			if (minDiscountPercent != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("discountPercent"), minDiscountPercent));
			}
			if (bookId != null) {
				// Join simple: un libro distinto no duplica la promocion.
				Join<Promotion, Book> book = root.join("books");
				predicates.add(cb.equal(book.get("id"), bookId));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}
}
