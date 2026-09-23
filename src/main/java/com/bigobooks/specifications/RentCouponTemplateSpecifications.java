package com.bigobooks.specifications;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.bigobooks.entities.rents.RentCouponTemplate;

import org.springframework.data.jpa.domain.Specification;

/**
 * Especificaciones de busqueda para plantillas de cupones de renta.
 */
public final class RentCouponTemplateSpecifications {

	private RentCouponTemplateSpecifications() {
	}

	public static Specification<RentCouponTemplate> build(String code, Boolean active,
			Integer minDiscountPercentage) {
		List<Specification<RentCouponTemplate>> specs = new ArrayList<>();
		if (code != null && !code.isBlank()) {
			specs.add(byCodeContaining(code.trim()));
		}
		if (Boolean.TRUE.equals(active)) {
			specs.add(byActive());
		}
		if (minDiscountPercentage != null) {
			specs.add(byMinDiscountPercentage(minDiscountPercentage));
		}
		return Specification.allOf(specs);
	}

	public static Specification<RentCouponTemplate> byCodeContaining(String code) {
		return (root, query, cb) -> cb.like(root.get("code"), "%" + code + "%");
	}

	public static Specification<RentCouponTemplate> byActive() {
		return (root, query, cb) -> {
			LocalDateTime now = LocalDateTime.now();
			return cb.and(
					cb.or(cb.isNull(root.get("startDate")), cb.lessThanOrEqualTo(root.get("startDate"), now)),
					cb.or(cb.isNull(root.get("endDate")), cb.greaterThanOrEqualTo(root.get("endDate"), now)));
		};
	}

	public static Specification<RentCouponTemplate> byMinDiscountPercentage(Integer min) {
		return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("discountPercentage"), min);
	}
}