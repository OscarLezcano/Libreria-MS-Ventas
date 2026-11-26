package com.bigobooks.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bigobooks.entities.auth.UserAccount;
import com.bigobooks.entities.book.Book;
import com.bigobooks.entities.orders.Coupon;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.entities.orders.OrderStatus;
import com.bigobooks.repositories.OrderRepository;
import com.bigobooks.util.exception.ConflictException;

@ExtendWith(MockitoExtension.class)
class OrderCouponServiceTest {

	@Mock
	private CouponService couponService;

	@Mock
	private OrderRepository orderRepository;

	@InjectMocks
	private OrderCouponService orderCouponService;

	@Test
	void cuponGlobalAplicaSobreTodoElPedido() {
		Coupon coupon = coupon(10, null);
		when(couponService.requireByCode("GLOBAL")).thenReturn(coupon);
		Order order = order(detail(1L, 2, 100));

		orderCouponService.applyCoupon(order, "GLOBAL");

		assertEquals(200, order.getSubtotal());
		assertEquals(20, order.getDiscountAmount());
		assertEquals(180, order.getTotalPrice());
	}

	@Test
	void cuponConLibrosSoloDescuentaLosLibrosElegibles() {
		Coupon coupon = coupon(10, List.of(book(1L)));
		when(couponService.requireByCode("LIBRO")).thenReturn(coupon);
		Order order = order(detail(1L, 2, 100), detail(2L, 1, 300));

		orderCouponService.applyCoupon(order, "LIBRO");

		assertEquals(500, order.getSubtotal());
		assertEquals(20, order.getDiscountAmount());
		assertEquals(480, order.getTotalPrice());
	}

	@Test
	void cuponConLibrosRechazaPedidosSinLibrosElegibles() {
		Coupon coupon = coupon(10, List.of(book(1L)));
		when(couponService.requireByCode("LIBRO")).thenReturn(coupon);
		Order order = order(detail(2L, 1, 300));

		assertThrows(IllegalArgumentException.class, () -> orderCouponService.applyCoupon(order, "LIBRO"));
	}

	@Test
	void rechazaCuponVencido() {
		Coupon coupon = coupon(10, null);
		coupon.setValidUntil(LocalDateTime.now().minusDays(1));
		when(couponService.requireByCode("VIEJO")).thenReturn(coupon);
		Order order = order(detail(1L, 1, 100));

		assertThrows(IllegalArgumentException.class, () -> orderCouponService.applyCoupon(order, "VIEJO"));
	}

	@Test
	void rechazaCuponNoVigente() {
		Coupon coupon = coupon(10, null);
		coupon.setValidFrom(LocalDateTime.now().plusDays(1));
		when(couponService.requireByCode("FUTURO")).thenReturn(coupon);
		Order order = order(detail(1L, 1, 100));

		assertThrows(IllegalArgumentException.class, () -> orderCouponService.applyCoupon(order, "FUTURO"));
	}

	@Test
	void rechazaSegundoUsoPorUsuario() {
		Coupon coupon = coupon(10, null);
		coupon.setSingleUsePerUser(true);
		when(couponService.requireByCode("UNICO")).thenReturn(coupon);
		when(orderRepository.countActiveCouponUsages(anyLong(), anyString(), anyLong(), any())).thenReturn(1L);
		Order order = order(detail(1L, 1, 100));

		assertThrows(ConflictException.class, () -> orderCouponService.applyCoupon(order, "UNICO"));
	}

	@Test
	void permitePrimerUsoPorUsuario() {
		Coupon coupon = coupon(10, null);
		coupon.setSingleUsePerUser(true);
		when(couponService.requireByCode("UNICO")).thenReturn(coupon);
		when(orderRepository.countActiveCouponUsages(anyLong(), anyString(), anyLong(), any())).thenReturn(0L);
		Order order = order(detail(1L, 1, 100));

		orderCouponService.applyCoupon(order, "UNICO");

		assertEquals(10, order.getDiscountAmount());
	}

	private Coupon coupon(int percent, List<Book> books) {
		Coupon coupon = new Coupon();
		coupon.setCode("TEST");
		coupon.setDiscountPercent(percent);
		coupon.setBooks(books == null ? List.of() : books);
		return coupon;
	}

	private Book book(Long id) {
		Book book = new Book();
		book.setId(id);
		return book;
	}

	private OrderDetail detail(Long bookId, int quantity, long unitPrice) {
		OrderDetail detail = new OrderDetail();
		detail.setBookId(bookId);
		detail.setQuantity(quantity);
		detail.setUnitPrice(unitPrice);
		return detail;
	}

	private Order order(OrderDetail... details) {
		Order order = new Order();
		order.setId(10L);
		order.setStatus(OrderStatus.PENDING);
		UserAccount user = new UserAccount();
		user.setId(5L);
		order.setUserAccount(user);
		order.setOrderDetails(List.of(details));
		return order;
	}
}
