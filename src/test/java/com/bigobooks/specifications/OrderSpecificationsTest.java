package com.bigobooks.specifications;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import com.bigobooks.entities.orders.Order;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

class OrderSpecificationsTest {

	@Test
	@SuppressWarnings("unchecked")
	void appliesTotalRangeToTheQuery() {
		Root<Order> root = mock(Root.class);
		CriteriaQuery<?> query = mock(CriteriaQuery.class);
		CriteriaBuilder builder = mock(CriteriaBuilder.class);
		Path<Long> totalPrice = mock(Path.class);
		Predicate minimum = mock(Predicate.class);
		Predicate maximum = mock(Predicate.class);
		when(root.<Long>get("totalPrice")).thenReturn(totalPrice);
		when(builder.greaterThanOrEqualTo(totalPrice, 1_000L)).thenReturn(minimum);
		when(builder.lessThanOrEqualTo(totalPrice, 2_000L)).thenReturn(maximum);

		Specification<Order> specification = OrderSpecifications.build(null, null, null, null, null, null, null,
				null, 1_000L, 2_000L);
		specification.toPredicate(root, query, builder);

		verify(builder).greaterThanOrEqualTo(totalPrice, 1_000L);
		verify(builder).lessThanOrEqualTo(totalPrice, 2_000L);
	}
}
