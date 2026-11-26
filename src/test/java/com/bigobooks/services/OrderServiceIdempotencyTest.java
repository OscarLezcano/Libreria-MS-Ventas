package com.bigobooks.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bigobooks.dto.OrderCreateRequest;
import com.bigobooks.dto.OrderItemCreate;
import com.bigobooks.entities.auth.UserAccount;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderStatus;
import com.bigobooks.repositories.OrderRepository;
import com.bigobooks.util.exception.ConflictException;
import com.bigobooks.util.exception.NotFoundException;

class OrderServiceIdempotencyTest {

	private OrderRepository repository;
	private StockService stockService;
	private OrderRequestFingerprint fingerprint;
	private OrderService service;

	@BeforeEach
	void setUp() {
		repository = mock(OrderRepository.class);
		stockService = mock(StockService.class);
		fingerprint = new OrderRequestFingerprint();
		service = new OrderService(repository, mock(OrderDetailService.class), mock(UserAccountService.class),
				mock(WarehouseService.class), stockService, mock(OrderCouponService.class), mock(BookService.class),
				fingerprint, mock(MailOutboxService.class));
	}

	@Test
	void sameKeyAndPayloadReturnsOriginalOrder() {
		OrderCreateRequest request = request(3L, 2);
		Order original = order(1L, OrderStatus.PENDING, "12345678");
		original.setRequestHash(fingerprint.of(request));

		when(repository.findByIdempotencyKeyAndUserId("checkout-1", 1L)).thenReturn(Optional.of(original));
		when(repository.findByIdWithDetails(1L)).thenReturn(Optional.of(original));

		Order result = service.createFromRequest(request, 1L, "checkout-1");

		assertSame(original, result);
		verify(repository, never()).save(any(Order.class));
	}

	@Test
	void sameKeyWithDifferentPayloadIsRejected() {
		OrderCreateRequest request = request(3L, 2);
		Order original = order(1L, OrderStatus.PENDING, "12345678");
		original.setRequestHash("otro-hash");

		when(repository.findByIdempotencyKeyAndUserId("checkout-1", 1L)).thenReturn(Optional.of(original));

		assertThrows(ConflictException.class,
				() -> service.createFromRequest(request, 1L, "checkout-1"));
		verify(repository, never()).save(any(Order.class));
	}

	@Test
	void payingAnAlreadyPaidOrderIsIdempotent() {
		Order paid = order(1L, OrderStatus.PAID, "12345678");
		when(repository.findByNumeroOrdenForUpdate("12345678")).thenReturn(Optional.of(paid));

		Order result = service.changeOwnStatus("12345678", 1L, OrderStatus.PAID);

		assertSame(paid, result);
		verify(stockService, never()).decreaseStock(any(Order.class));
		verify(repository, never()).save(any(Order.class));
	}

	@Test
	void anotherUsersOrderIsHidden() {
		Order paid = order(1L, OrderStatus.PAID, "12345678");
		when(repository.findByNumeroOrdenForUpdate("12345678")).thenReturn(Optional.of(paid));

		assertThrows(NotFoundException.class,
				() -> service.changeOwnStatus("12345678", 2L, OrderStatus.PAID));
	}

	@Test
	void invalidNumeroOrdenIsRejectedBeforeQuerying() {
		assertThrows(IllegalArgumentException.class,
				() -> service.changeOwnStatus("123", 1L, OrderStatus.PAID));
		verify(repository, never()).findByNumeroOrdenForUpdate(any());
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
		OrderItemCreate item = new OrderItemCreate();
		item.setBookId(bookId);
		item.setQuantity(quantity);
		request.setItems(List.of(item));
		return request;
	}

	private Order order(Long id, OrderStatus status, String numeroOrden) {
		Order order = new Order();
		order.setId(id);
		order.setStatus(status);
		order.setNumeroOrden(numeroOrden);
		UserAccount user = new UserAccount();
		user.setId(1L);
		order.setUserAccount(user);
		return order;
	}
}
