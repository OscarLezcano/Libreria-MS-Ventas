package com.bigobooks.services;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.bigobooks.entities.orders.Coupon;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.entities.orders.OrderStatus;
import com.bigobooks.exception.ConflictException;

/**
 * Reglas de negocio del cupon dentro de una venta: aplicacion, baja y
 * recalculo de subtotales, descuento y total.
 */
@Service
public class OrderCouponService {

	private static final Logger log = LoggerFactory.getLogger(OrderCouponService.class);

	private final CouponService couponService;

	public OrderCouponService(CouponService couponService) {
		this.couponService = couponService;
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
		if (coupon.getValidUntil() != null && coupon.getValidUntil().isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("El cupon " + coupon.getCode() + " esta vencido");
		}
		order.setCoupon(coupon);
		recalculate(order);
		log.debug("Cupon {} aplicado a la venta {}", coupon.getCode(), order.getId());
	}

	/**
	 * Quita el cupon de la venta y recalcula el total.
	 */
	public void removeCoupon(Order order) {
		order.setCoupon(null);
		recalculate(order);
		log.debug("Cupon quitado de la venta {}", order.getId());
	}

	/**
	 * Recalcula subtotal, descuento y total de la venta.
	 */
	public void recalculate(Order order) {
		long subtotal = 0;
		if (order.getOrderDetails() != null) {
			for (OrderDetail detail : order.getOrderDetails()) {
				subtotal += detail.getUnitPrice() * detail.getQuantity() - detail.getDiscount();
			}
		}
		long discount = 0;
		if (order.getCoupon() != null) {
			discount = subtotal * order.getCoupon().getDiscountPercent() / 100;
		}
		order.setSubtotal(subtotal);
		order.setDiscountAmount(discount);
		order.setTotalPrice(subtotal - discount);
	}
}
