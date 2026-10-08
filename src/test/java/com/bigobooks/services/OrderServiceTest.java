package com.bigobooks.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bigobooks.dto.OrderCreateRequest;
import com.bigobooks.dto.OrderItemCreate;
import com.bigobooks.dto.OrderUpdateRequest;
import com.bigobooks.entities.auth.UserAccount;
import com.bigobooks.entities.book.Book;
import com.bigobooks.entities.orders.Coupon;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.entities.orders.OrderStatus;
import com.bigobooks.exception.ConflictException;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.repositories.OrderDetailRepository;
import com.bigobooks.repositories.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private OrderRepository repository;
	@Mock
	private OrderDetailRepository orderDetailRepository;
	@Mock
	private BookService bookService;
	@Mock
	private UserAccountService userAccountService;
	@Mock
	private WarehouseService warehouseService;
	@Mock
	private StockService stockService;
	@Mock
	private CouponService couponService;

	private OrderCouponService orderCouponService;
	private OrderDetailService orderDetailService;
	private OrderService orderService;

	@BeforeEach
	void setUp() {
		orderCouponService = new OrderCouponService(couponService);
		orderDetailService = new OrderDetailService(orderDetailRepository, bookService);
		orderService = new OrderService(repository, orderDetailService, userAccountService, warehouseService,
				stockService, orderCouponService);
	}

	@Test
	void createFromRequestCalculaTotales() {
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		Book book = new Book();
		book.setId(10L);
		book.setTitle("El principito");
		book.setPrice(1000L);
		when(bookService.requireById(10L)).thenReturn(book);

		OrderItemCreate item = new OrderItemCreate();
		item.setBookId(10L);
		item.setQuantity(2);
		OrderCreateRequest request = new OrderCreateRequest();
		request.setItems(List.of(item));

		Order order = orderService.createFromRequest(request);

		assertEquals(OrderStatus.PENDING, order.getStatus());
		assertEquals(2000L, order.getSubtotal());
		assertEquals(0L, order.getDiscountAmount());
		assertEquals(2000L, order.getTotalPrice());
		assertEquals(1, order.getOrderDetails().size());
		assertEquals("El principito", order.getOrderDetails().get(0).getBookName());
		assertEquals(1000L, order.getOrderDetails().get(0).getUnitPrice());
		verify(orderDetailRepository).save(any());
	}

	@Test
	void createFromRequestAplicaCuponSobreElTotal() {
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		Book book = new Book();
		book.setId(10L);
		book.setTitle("Libro");
		book.setPrice(2000L);
		when(bookService.requireById(10L)).thenReturn(book);
		Coupon coupon = new Coupon();
		coupon.setId(9L);
		coupon.setCode("VERANO");
		coupon.setDiscountPercent(10);
		when(couponService.requireByCode("VERANO")).thenReturn(coupon);

		OrderItemCreate item = new OrderItemCreate();
		item.setBookId(10L);
		item.setQuantity(1);
		OrderCreateRequest request = new OrderCreateRequest();
		request.setItems(List.of(item));
		request.setCouponCode("VERANO");

		Order order = orderService.createFromRequest(request);

		assertEquals(2000L, order.getSubtotal());
		assertEquals(200L, order.getDiscountAmount());
		assertEquals(1800L, order.getTotalPrice());
		assertEquals(coupon, order.getCoupon());
	}

	@Test
	void createFromRequestRequiereItems() {
		OrderCreateRequest request = new OrderCreateRequest();
		request.setItems(List.of());

		assertThrows(IllegalArgumentException.class, () -> orderService.createFromRequest(request));
	}

	@Test
	void createFromRequestRechazaCantidadNoPositiva() {
		OrderItemCreate item = new OrderItemCreate();
		item.setBookId(10L);
		item.setQuantity(0);
		OrderCreateRequest request = new OrderCreateRequest();
		request.setItems(List.of(item));

		assertThrows(IllegalArgumentException.class, () -> orderService.createFromRequest(request));
		verify(bookService, never()).requireById(any());
	}

	@Test
	void createFromRequestValidaUsuario() {
		when(userAccountService.requireById(7L)).thenThrow(new NotFoundException("No existe el usuario con id 7"));

		OrderItemCreate item = new OrderItemCreate();
		item.setBookId(10L);
		item.setQuantity(1);
		OrderCreateRequest request = new OrderCreateRequest();
		request.setItems(List.of(item));
		request.setUserId(7L);

		assertThrows(NotFoundException.class, () -> orderService.createFromRequest(request));
	}

	@Test
	void changeStatusPendienteAPagadoDescuentaStock() {
		Order order = orderWithDetail(OrderStatus.PENDING);
		when(repository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Order result = orderService.changeStatus(1L, OrderStatus.PAID);

		assertEquals(OrderStatus.PAID, result.getStatus());
		verify(stockService).decreaseStock(order);
		verify(stockService, never()).restoreStock(any());
	}

	@Test
	void changeStatusPagadoAReembolsadoDevuelveStock() {
		Order order = orderWithDetail(OrderStatus.PAID);
		when(repository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Order result = orderService.changeStatus(1L, OrderStatus.REFUNDED);

		assertEquals(OrderStatus.REFUNDED, result.getStatus());
		verify(stockService).restoreStock(order);
		verify(stockService, never()).decreaseStock(any());
	}

	@Test
	void changeStatusRechazaTransicionInvalida() {
		Order order = orderWithDetail(OrderStatus.CANCELLED);
		when(repository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

		assertThrows(ConflictException.class, () -> orderService.changeStatus(1L, OrderStatus.PAID));
		verify(stockService, never()).decreaseStock(any());
		verify(repository, never()).save(any());
	}

	@Test
	void applyCouponRecalculaElTotal() {
		Order order = orderWithDetail(OrderStatus.PENDING);
		when(repository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		Coupon coupon = new Coupon();
		coupon.setId(9L);
		coupon.setCode("VERANO");
		coupon.setDiscountPercent(10);
		when(couponService.requireByCode("VERANO")).thenReturn(coupon);

		Order result = orderService.applyCoupon(1L, "VERANO");

		assertEquals(coupon, result.getCoupon());
		assertEquals(100L, result.getDiscountAmount());
		assertEquals(900L, result.getTotalPrice());
	}

	@Test
	void applyCouponRechazaSiLaVentaNoEstaPendiente() {
		Order order = orderWithDetail(OrderStatus.PAID);
		when(repository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

		assertThrows(ConflictException.class, () -> orderService.applyCoupon(1L, "VERANO"));
		verify(couponService, never()).requireByCode(any());
	}

	@Test
	void removeCouponRecalculaElTotal() {
		Order order = orderWithDetail(OrderStatus.PENDING);
		Coupon coupon = new Coupon();
		coupon.setDiscountPercent(50);
		order.setCoupon(coupon);
		when(repository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		Order result = orderService.removeCoupon(1L);

		assertNull(result.getCoupon());
		assertEquals(0L, result.getDiscountAmount());
		assertEquals(1000L, result.getTotalPrice());
	}

	@Test
	void deleteEsBorradoLogico() {
		Order order = orderWithDetail(OrderStatus.PENDING);
		when(repository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		assertFalse(order.isDeleted());

		orderService.delete(1L);

		assertTrue(order.isDeleted());
		verify(repository).save(order);
	}

	@Test
	void requireByIdLanzaNotFound() {
		when(repository.findByIdWithDetails(44L)).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> orderService.requireById(44L));
	}

	@Test
	void updateSoloPermiteVentasPendientes() {
		Order order = orderWithDetail(OrderStatus.PAID);
		when(repository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

		OrderUpdateRequest request = new OrderUpdateRequest();
		request.setInvoiceNumber("F-001");

		assertThrows(ConflictException.class, () -> orderService.update(1L, request));
	}

	@Test
	void updateCambiaFacturaDeUnaVentaPendiente() {
		Order order = orderWithDetail(OrderStatus.PENDING);
		when(repository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		UserAccount user = new UserAccount();
		user.setId(7L);
		when(userAccountService.requireById(7L)).thenReturn(user);

		OrderUpdateRequest request = new OrderUpdateRequest();
		request.setInvoiceNumber("F-001");
		request.setUserId(7L);

		Order result = orderService.update(1L, request);

		assertEquals("F-001", result.getInvoiceNumber());
		assertEquals(user, result.getUserAccount());
	}

	private Order orderWithDetail(OrderStatus status) {
		Order order = new Order();
		order.setId(1L);
		order.setStatus(status);
		order.setWarehouseId(5L);
		order.setOrderDetails(new ArrayList<>());
		OrderDetail detail = new OrderDetail();
		detail.setBookId(10L);
		detail.setBookName("Libro");
		detail.setQuantity(1);
		detail.setUnitPrice(1000L);
		detail.setDiscount(0L);
		order.getOrderDetails().add(detail);
		return order;
	}
}
