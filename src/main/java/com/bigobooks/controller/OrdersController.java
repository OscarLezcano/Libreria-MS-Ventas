package com.bigobooks.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.bigobooks.dto.Envelope;
import com.bigobooks.dto.OrderClientStatusChange;
import com.bigobooks.dto.OrderCreateRequest;
import com.bigobooks.dto.OrderDto;
import com.bigobooks.dto.OrderStatusChange;
import com.bigobooks.dto.OrderUpdateRequest;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderStatus;
import com.bigobooks.mappers.OrderMapper;
import com.bigobooks.security.CurrentUser;
import com.bigobooks.services.BookService;
import com.bigobooks.services.OrderQueryService;
import com.bigobooks.services.OrderService;
import com.bigobooks.util.Envelopes;

/**
 * Controlador de ventas: implementa la interfaz generada desde openapi.yaml.
 * Las rutas por numeroOrden son el contrato publico; las rutas por id quedan
 * restringidas a ADMIN/SELLER.
 */
@RestController
public class OrdersController implements OrdersApi {

	private static final Logger log = LoggerFactory.getLogger(OrdersController.class);

	private final OrderService orderService;
	private final OrderQueryService orderQueryService;
	private final OrderMapper orderMapper;
	private final CurrentUser currentUser;
	private final BookService bookService;

	public OrdersController(OrderService orderService, OrderQueryService orderQueryService, OrderMapper orderMapper,
			CurrentUser currentUser, BookService bookService) {
		this.orderService = orderService;
		this.orderQueryService = orderQueryService;
		this.orderMapper = orderMapper;
		this.currentUser = currentUser;
		this.bookService = bookService;
	}

	@GetMapping("/orders/transaction-demo/direct/{propagation}")
	@PreAuthorize("@roles.allows('orders-view')")
	public ResponseEntity<Map<String, String>> transactionDirect(@PathVariable String propagation) {
		return ResponseEntity.ok(Map.of("mode", "direct", "result", invokeDirect(propagation)));
	}

	@GetMapping("/orders/transaction-demo/from-parent/{propagation}")
	@PreAuthorize("@roles.allows('orders-view')")
	public ResponseEntity<Map<String, String>> transactionFromParent(@PathVariable String propagation) {
		return ResponseEntity.ok(Map.of("mode", "from-parent", "result",
				invokeFromParent(propagation)));
	}

	private String invokeDirect(String propagation) {
		try {
			return switch (propagation.trim().toUpperCase()) {
			case "REQUIRED" -> bookService.transactionRequired();
			case "REQUIRES_NEW" -> bookService.transactionRequiresNew();
			case "SUPPORTS" -> bookService.transactionSupports();
			case "NOT_SUPPORTED" -> bookService.transactionNotSupported();
			case "NEVER" -> bookService.transactionNever();
			case "MANDATORY" -> bookService.transactionMandatory();
			default -> throw new IllegalArgumentException("Propagacion no soportada: " + propagation);
			};
		} catch (RuntimeException exception) {
			log.warn("TX-DEMO {} mode=direct exception={} message={}", propagation,
					exception.getClass().getSimpleName(), exception.getMessage());
			return "EXCEPTION " + exception.getClass().getSimpleName() + ": " + exception.getMessage();
		}
	}

	private String invokeFromParent(String propagation) {
		try {
			return orderService.transactionPropagationDemo(propagation);
		} catch (RuntimeException exception) {
			log.warn("TX-DEMO {} mode=from-parent exception={} message={}", propagation,
					exception.getClass().getSimpleName(), exception.getMessage());
			return "EXCEPTION " + exception.getClass().getSimpleName() + ": " + exception.getMessage();
		}
	}

	@Override
	@PreAuthorize("@roles.allows('orders-create')")
	public ResponseEntity<Envelope> createOrder(OrderCreateRequest orderCreateRequest, String idempotencyKey) {
		log.info("POST /orders: creacion de venta recibida");
		requireIdempotencyKey(idempotencyKey);
		Long authenticatedUserId = currentUser.id();
		Long requestedUserId = orderCreateRequest == null ? null : orderCreateRequest.getUserId();
		// El staff puede registrar la venta a nombre de un cliente; el cliente
		// siempre compra para si mismo (id tomado del token).
		Long effectiveUserId = currentUser.hasAnyRole("ADMIN", "SELLER") && requestedUserId != null
				? requestedUserId
				: authenticatedUserId;
		Order created = orderService.createFromRequest(orderCreateRequest, effectiveUserId, idempotencyKey);
		return new ResponseEntity<>(Envelopes.single(orderMapper.toDto(created)), HttpStatus.CREATED);
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN','SELLER')")
	public ResponseEntity<Envelope> listOrders(String status, Long warehouseId, Long userId, String couponCode,
			Long bookId, LocalDateTime createdFrom, LocalDateTime createdTo, Integer page, Integer size,
			String sort) {
		log.info("GET /orders: listado administrativo de ventas (page={}, size={}, sort={})", page, size, sort);
		Page<Order> orders = orderService.list(status, userId, warehouseId, couponCode, bookId, createdFrom,
				createdTo, page, size, sort);
		List<OrderDto> data = orders.map(orderMapper::toDto).getContent();
		return ResponseEntity.ok(Envelopes.page(orders, data));
	}

	@Override
	@PreAuthorize("@roles.allows('orders-view')")
	public ResponseEntity<Envelope> listMyOrders(String status, LocalDateTime createdFrom, LocalDateTime createdTo,
			String search, Integer page, Integer size, String sort) {
		Long userId = currentUser.id();
		log.info("GET /orders/mine: listado de ventas del usuario {}", userId);
		Page<Order> orders = orderService.listByUser(userId, status, createdFrom, createdTo, page, size, sort,
				search);
		List<OrderDto> data = orders.map(orderMapper::toDto).getContent();
		return ResponseEntity.ok(Envelopes.page(orders, data));
	}

	@Override
	@PreAuthorize("@roles.allows('orders-view')")
	public ResponseEntity<Envelope> getOrderByNumeroOrden(String numeroOrden) {
		Long userId = currentUser.id();
		log.info("GET /orders/numero/{}: pedido propio", numeroOrden);
		return ResponseEntity.ok(Envelopes.single(orderQueryService.getByNumeroOrden(numeroOrden, userId)));
	}

	@Override
	@PreAuthorize("@roles.allows('orders-view')")
	public ResponseEntity<Envelope> getOrderDetailsByNumeroOrden(String numeroOrden) {
		Long userId = currentUser.id();
		log.info("GET /orders/numero/{}/details: desglose del pedido propio", numeroOrden);
		return ResponseEntity.ok(Envelopes.single(orderQueryService.getDetailsByNumeroOrden(numeroOrden, userId)));
	}

	@Override
	@PreAuthorize("@roles.allows('orders-status')")
	public ResponseEntity<Envelope> changeOrderStatusByNumeroOrden(String numeroOrden,
			OrderClientStatusChange orderClientStatusChange) {
		Long userId = currentUser.id();
		OrderClientStatusChange.StatusEnum status = orderClientStatusChange == null
				? null : orderClientStatusChange.getStatus();
		OrderStatus newStatus = status == null ? null : OrderStatus.valueOf(status.name());
		log.info("PATCH /orders/numero/{}/status: cambio de estado recibido", numeroOrden);
		Order changed = orderService.changeOwnStatus(numeroOrden, userId, newStatus);
		return ResponseEntity.ok(Envelopes.single(orderMapper.toDto(changed)));
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN','SELLER')")
	public ResponseEntity<Envelope> getOrderById(Long id) {
		log.info("GET /orders/{}: detalle administrativo de venta", id);
		return ResponseEntity.ok(Envelopes.single(orderQueryService.getById(id)));
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN','SELLER')")
	public ResponseEntity<Envelope> getOrderDetails(Long id) {
		log.info("GET /orders/{}/details: desglose administrativo de venta", id);
		return ResponseEntity.ok(Envelopes.single(orderQueryService.getDetails(id)));
	}

	@Override
	@PreAuthorize("@roles.allows('orders-view')")
	public ResponseEntity<Envelope> listBestSellingBooks(Integer limit) {
		log.info("GET /orders/best-sellers: top {} libros mas vendidos", limit);
		return ResponseEntity.ok(Envelopes.single(orderQueryService.getBestSellingBooks(limit)));
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN','SELLER')")
	public ResponseEntity<Envelope> updateOrder(Long id, OrderUpdateRequest orderUpdateRequest) {
		log.info("PUT /orders/{}: actualizacion administrativa de venta recibida", id);
		return ResponseEntity.ok(Envelopes.single(orderMapper.toDto(orderService.update(id, orderUpdateRequest))));
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN','SELLER')")
	public ResponseEntity<Envelope> changeOrderStatus(Long id, OrderStatusChange orderStatusChange) {
		log.info("PATCH /orders/{}/status: cambio de estado administrativo recibido", id);
		OrderStatusChange.StatusEnum status = orderStatusChange == null ? null : orderStatusChange.getStatus();
		OrderStatus newStatus = status == null ? null : OrderStatus.valueOf(status.name());
		return ResponseEntity.ok(Envelopes.single(orderMapper.toDto(orderService.changeStatus(id, newStatus))));
	}

	@Override
	@PreAuthorize("hasAnyRole('ADMIN','SELLER')")
	public ResponseEntity<Void> deleteOrder(Long id) {
		log.info("DELETE /orders/{}: borrado logico administrativo de venta recibido", id);
		orderService.delete(id);
		return ResponseEntity.noContent().build();
	}

	private void requireIdempotencyKey(String idempotencyKey) {
		if (currentUser.hasAnyRole("CLIENT") && (idempotencyKey == null || idempotencyKey.isBlank())) {
			throw new IllegalArgumentException("La cabecera Idempotency-Key es obligatoria para clientes");
		}
		if (idempotencyKey != null && idempotencyKey.length() > 255) {
			throw new IllegalArgumentException("La cabecera Idempotency-Key es demasiado larga");
		}
	}
}
