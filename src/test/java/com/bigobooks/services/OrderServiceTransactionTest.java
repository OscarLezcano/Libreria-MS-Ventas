package com.bigobooks.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class OrderServiceTransactionTest {

	@Test
	void paginatedQueriesKeepTheEntityManagerOpenUntilDetailsAreInitialized() {
		List<Method> methods = Arrays.stream(OrderService.class.getDeclaredMethods())
				.filter(method -> method.getName().equals("list") || method.getName().equals("listByUser"))
				.toList();

		assertFalse(methods.isEmpty());
		for (Method method : methods) {
			Transactional transaction = method.getAnnotation(Transactional.class);
			assertNotNull(transaction, method.getName() + " debe ser transaccional");
			assertEquals(Propagation.REQUIRED, transaction.propagation(), method.getName()
					+ " debe usar REQUIRED para mantener el EntityManager abierto");
			assertTrue(transaction.readOnly(), method.getName() + " debe ser readOnly");
		}
	}
}
