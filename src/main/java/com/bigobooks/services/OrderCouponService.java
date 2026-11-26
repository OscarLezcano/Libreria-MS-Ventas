package com.bigobooks.services;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.bigobooks.entities.book.Book;
import com.bigobooks.entities.orders.Coupon;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.entities.orders.OrderStatus;
import com.bigobooks.repositories.OrderRepository;
import com.bigobooks.util.exception.ConflictException;

/**
 * Reglas de negocio del cupon dentro de una venta: aplicacion, baja y
 * recalculo de subtotales, descuento y total.
 */
@Service
public class OrderCouponService {

	private static final Logger log = LoggerFactory.getLogger(OrderCouponService.class);

	private static final List<OrderStatus> ACTIVE_REDEMPTION_STATUSES = List.of(OrderStatus.PENDING,
			OrderStatus.PAID);

	private final CouponService couponService;

	private final OrderRepository orderRepository;

	public OrderCouponService(CouponService couponService, OrderRepository orderRepository) {
		this.couponService = couponService;
		this.orderRepository = orderRepository;
	}

	/**
	 * Aplica el cupon a la venta y recalcula el total.
	 */
	public void applyCoupon(Order order, String couponCode) {
		if (couponCode == null || couponCode.isBlank()) {
			throw new IllegalArgumentException("Debe indicar el codigo del cupon");
		}
		if (order.getStatus() != OrderStatus.PENDING) {
			throw new ConflictException("Solo se pueden aplicar cupones a ventas en estado PENDING");
		}
		Coupon coupon = couponService.requireByCode(couponCode);
		LocalDateTime now = LocalDateTime.now();
		if (coupon.getValidFrom() != null && coupon.getValidFrom().isAfter(now)) {
			throw new IllegalArgumentException("El cupon " + coupon.getCode() + " todavia no esta vigente");
		}
		if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(now)) {
			throw new IllegalArgumentException("El cupon " + coupon.getCode() + " esta vencido");
		}
		if (coupon.getDiscountPercent() < 0 || coupon.getDiscountPercent() > 100) {
			throw new IllegalArgumentException("El porcentaje del cupon no es valido");
		}
		validateSingleUse(order, coupon);
		order.setCoupon(coupon);
		order.setCouponCode(coupon.getCode());
		recalculate(order);
		log.debug("Cupon {} aplicado a la venta {}", coupon.getCode(), order.getId());
	}

	/**
	 * Recalcula subtotal, descuento y total de la venta. Si el cupon tiene
	 * libros asociados, el descuento se aplica solo sobre esos libros.
	 */
	public void recalculate(Order order) {
		long subtotal = 0;
		if (order.getOrderDetails() != null) {
			for (OrderDetail detail : order.getOrderDetails()) {
				long lineTotal = calculateLineTotal(detail);
				if (lineTotal < 0) {
					throw new IllegalArgumentException("El descuento del item no puede superar el precio del libro");
				}
				subtotal += lineTotal;
			}
		}
		long discount = calculateCouponDiscount(order, subtotal);
		order.setSubtotal(subtotal);
		order.setDiscountAmount(discount);
		order.setTotalPrice(subtotal - discount);
	}

	private long calculateCouponDiscount(Order order, long subtotal) {
		Coupon coupon = order.getCoupon();
		if (coupon == null) {
			return 0;
		}
		Set<Long> eligibleBookIds = new HashSet<>();
		if (coupon.getBooks() != null) {
			for (Book book : coupon.getBooks()) {
				eligibleBookIds.add(book.getId());
			}
		}
		long discountBase = subtotal;
		if (!eligibleBookIds.isEmpty()) {
			discountBase = 0;
			if (order.getOrderDetails() != null) {
				for (OrderDetail detail : order.getOrderDetails()) {
					if (detail.getBookId() != null && eligibleBookIds.contains(detail.getBookId())) {
						discountBase += calculateLineTotal(detail);
					}
				}
			}
			if (discountBase <= 0) {
				throw new IllegalArgumentException(
						"El cupon " + coupon.getCode() + " no aplica a los libros de este pedido");
			}
		}
		return discountBase * coupon.getDiscountPercent() / 100;
	}

	private void validateSingleUse(Order order, Coupon coupon) {
		if (!coupon.isSingleUsePerUser() || order.getUserAccount() == null || order.getId() == null) {
			return;
		}
		long usages = orderRepository.countActiveCouponUsages(order.getUserAccount().getId(), coupon.getCode(),
				order.getId(), ACTIVE_REDEMPTION_STATUSES);
		if (usages > 0) {
			throw new ConflictException("El cupon " + coupon.getCode() + " ya fue utilizado por este usuario");
		}
	}

	/** Regla unica de calculo de linea, reutilizada por el mapper publico. */
	public static long calculateLineTotal(OrderDetail detail) {
		if (detail.getUnitPrice() < 0 || detail.getDiscount() < 0) {
			throw new IllegalArgumentException("Precio y descuento del item no pueden ser negativos");
		}
		return detail.getUnitPrice() * detail.getQuantity() - detail.getDiscount();
	}
}
