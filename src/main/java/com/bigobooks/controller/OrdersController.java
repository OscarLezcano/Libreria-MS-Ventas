package com.bigobooks.controller;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.bigobooks.dto.OrderCouponApply;
import com.bigobooks.dto.OrderCreateRequest;
import com.bigobooks.dto.OrderDto;
import com.bigobooks.dto.OrderStatusChange;
import com.bigobooks.dto.OrderUpdateRequest;
import com.bigobooks.dto.PageResponseDto;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderStatus;
import com.bigobooks.mappers.OrderMapper;
import com.bigobooks.services.OrderService;

/**
 * Controlador de ventas: implementa la interfaz generada desde openapi.yaml.
 */
@RestController
public class OrdersController implements OrdersApi {

	private static final Logger log = LoggerFactory.getLogger(OrdersController.class);

	private final OrderService orderService;
	private final OrderMapper orderMapper;

	public OrdersController(OrderService orderService, OrderMapper orderMapper) {
		this.orderService = orderService;
		this.orderMapper = orderMapper;
	}

	@Override
	public ResponseEntity<OrderDto> createOrder(OrderCreateRequest orderCreateRequest) {
		log.info("POST /orders: creacion de venta recibida");
		Order created = orderService.createFromRequest(orderCreateRequest);
		return new ResponseEntity<>(orderMapper.toDto(created), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<PageResponseDto> listOrders(String status, Long warehouseId, String couponCode, Long bookId,
			LocalDateTime createdFrom, LocalDateTime createdTo, Integer page, Integer size, String sort) {
		log.info("GET /orders: listado de ventas (page={}, size={}, sort={})", page, size, sort);
		Page<Order> orders = orderService.list(status, warehouseId, couponCode, bookId, createdFrom, createdTo, page,
				size, sort);
		return ResponseEntity.ok(PageResponseDto.from(orders.map(orderMapper::toDto)));
	}

	@Override
	public ResponseEntity<OrderDto> getOrderById(Long id) {
		log.info("GET /orders/{}: detalle de venta", id);
		return ResponseEntity.ok(orderMapper.toDto(orderService.requireById(id)));
	}

	@Override
	public ResponseEntity<OrderDto> updateOrder(Long id, OrderUpdateRequest orderUpdateRequest) {
		log.info("PUT /orders/{}: actualizacion de venta recibida", id);
		return ResponseEntity.ok(orderMapper.toDto(orderService.update(id, orderUpdateRequest)));
	}

	@Override
	public ResponseEntity<OrderDto> changeOrderStatus(Long id, OrderStatusChange orderStatusChange) {
		log.info("PUT /orders/{}/status: cambio de estado recibido", id);
		OrderStatusChange.StatusEnum status = orderStatusChange == null ? null : orderStatusChange.getStatus();
		OrderStatus newStatus = status == null ? null : OrderStatus.valueOf(status.name());
		return ResponseEntity.ok(orderMapper.toDto(orderService.changeStatus(id, newStatus)));
	}

	@Override
	public ResponseEntity<OrderDto> applyCouponToOrder(Long id, OrderCouponApply orderCouponApply) {
		log.info("POST /orders/{}/coupons: aplicar cupon recibido", id);
		String couponCode = orderCouponApply == null ? null : orderCouponApply.getCouponCode();
		return ResponseEntity.ok(orderMapper.toDto(orderService.applyCoupon(id, couponCode)));
	}

	@Override
	public ResponseEntity<Void> removeCouponFromOrder(Long id) {
		log.info("DELETE /orders/{}/coupons: quitar cupon recibido", id);
		orderService.removeCoupon(id);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<Void> deleteOrder(Long id) {
		log.info("DELETE /orders/{}: borrado logico de venta recibido", id);
		orderService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
