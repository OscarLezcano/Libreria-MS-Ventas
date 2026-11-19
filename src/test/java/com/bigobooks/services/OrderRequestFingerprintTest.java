package com.bigobooks.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.bigobooks.dto.OrderCreateRequest;
import com.bigobooks.dto.OrderItemCreate;

class OrderRequestFingerprintTest {

	private final OrderRequestFingerprint fingerprint = new OrderRequestFingerprint();

	@Test
	void samePayloadWithDifferentItemOrderHasSameHash() {
		OrderCreateRequest first = request(3L, 1);
		first.setItems(List.of(item(3L, 1), item(1L, 2)));
		OrderCreateRequest second = request(1L, 2);
		second.setItems(List.of(item(1L, 2), item(3L, 1)));

		assertEquals(fingerprint.of(first), fingerprint.of(second));
	}

	@Test
	void changedPayloadHasDifferentHash() {
		OrderCreateRequest original = request(3L, 1);
		OrderCreateRequest changed = request(3L, 2);

		assertNotEquals(fingerprint.of(original), fingerprint.of(changed));
	}

	private OrderCreateRequest request(Long bookId, int quantity) {
		OrderCreateRequest request = new OrderCreateRequest();
		request.setInvoiceRuc("80012345-0");
		request.setInvoiceName("Ana");
		request.setInvoiceLastName("Gomez");
		request.setInvoicePhone("0991234567");
		request.setShippingName("Ana");
		request.setShippingLastName("Gomez");
		request.setShippingDepartment("Central");
		request.setShippingCity("Asuncion");
		request.setShippingAddress("Calle 1");
		request.setShippingPhone("0991234567");
		request.setItems(List.of(item(bookId, quantity)));
		return request;
	}

	private OrderItemCreate item(Long bookId, int quantity) {
		OrderItemCreate item = new OrderItemCreate();
		item.setBookId(bookId);
		item.setQuantity(quantity);
		return item;
	}
}
