package com.bigobooks.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigobooks.dto.OrderCreateRequest;
import com.bigobooks.dto.OrderItemCreate;
import com.bigobooks.dto.OrderUpdateRequest;
import com.bigobooks.entities.auth.UserAccount;
import com.bigobooks.entities.book.Book;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.entities.orders.OrderStatus;
import com.bigobooks.exception.ConflictException;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.repositories.OrderDetailRepository;
import com.bigobooks.repositories.OrderRepository;
import com.bigobooks.service.BaseService;
import com.bigobooks.specifications.OrderSpecifications;

@Service
public class OrderService extends BaseService<Order, OrderRepository> {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);

	private final OrderDetailRepository orderDetailRepository;
	private final BookService bookService;
	private final UserAccountService userAccountService;
	private final WarehouseService warehouseService;
	private final StockService stockService;
	private final OrderCouponService orderCouponService;

	public OrderService(OrderRepository repository, OrderDetailRepository orderDetailRepository,
			BookService bookService, UserAccountService userAccountService, WarehouseService warehouseService,
			StockService stockService, OrderCouponService orderCouponService) {
		super(repository);
		this.orderDetailRepository = orderDetailRepository;
		this.bookService = bookService;
		this.userAccountService = userAccountService;
		this.warehouseService = warehouseService;
		this.stockService = stockService;
		this.orderCouponService = orderCouponService;
	}

	/** Venta con sus detalles ya cargados. */
	public Order requireById(Long id) {
		return getRepository().findByIdWithDetails(id)
				.orElseThrow(() -> new NotFoundException("No existe la venta con id " + id));
	}

	@Transactional
	public Order createFromRequest(OrderCreateRequest request) {
		if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
			throw new IllegalArgumentException("Debe incluir al menos un item en la venta");
		}
		if (request.getWarehouseId() != null) {
			warehouseService.requireById(request.getWarehouseId());
		}
		UserAccount user = request.getUserId() != null ? userAccountService.requireById(request.getUserId()) : null;

		Order order = new Order();
		order.setStatus(OrderStatus.PENDING);
		order.setWarehouseId(request.getWarehouseId());
		order.setUserAccount(user);
		order.setOrderDetails(new ArrayList<>());
		Order saved = getRepository().save(order);

		for (OrderItemCreate item : request.getItems()) {
			addDetail(saved, item);
		}
		orderCouponService.recalculate(saved);

		if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
			orderCouponService.applyCoupon(saved, request.getCouponCode());
		}

		Order result = getRepository().save(saved);
		log.info("Venta {} creada: {} item(s), subtotal={}, descuento={}, total={}", result.getId(),
				result.getOrderDetails().size(), result.getSubtotal(), result.getDiscountAmount(),
				result.getTotalPrice());
		return result;
	}

	@Transactional
	public Order update(Long id, OrderUpdateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Debe indicar los datos de la venta");
		}
		Order order = requireById(id);
		if (order.getStatus() != OrderStatus.PENDING) {
			throw new ConflictException("Solo se puede editar una venta en estado PENDING");
		}
		if (request.getWarehouseId() != null) {
			warehouseService.requireById(request.getWarehouseId());
			order.setWarehouseId(request.getWarehouseId());
		}
		if (request.getUserId() != null) {
			order.setUserAccount(userAccountService.requireById(request.getUserId()));
		}
		if (request.getBancardNumber() != null) {
			order.setBancardNumber(request.getBancardNumber());
		}
		if (request.getInvoiceNumber() != null) {
			order.setInvoiceNumber(request.getInvoiceNumber());
		}
		Order saved = getRepository().save(order);
		log.info("Venta {} actualizada", saved.getId());
		return saved;
	}

	/**
	 * Cambia el estado con las transiciones validas: PENDING-&gt;PAID,
	 * PENDING-&gt;CANCELLED y PAID-&gt;REFUNDED. El pago descuenta stock y el
	 * reembolso lo devuelve.
	 */
	@Transactional
	public Order changeStatus(Long id, OrderStatus newStatus) {
		if (newStatus == null) {
			throw new IllegalArgumentException("status es obligatorio");
		}
		Order order = requireById(id);
		OrderStatus current = order.getStatus();
		if (current == newStatus) {
			throw new ConflictException("La venta ya esta en el estado " + current);
		}
		if (!isAllowedTransition(current, newStatus)) {
			throw new ConflictException("Transicion no permitida: " + current + " -> " + newStatus);
		}
		if (newStatus == OrderStatus.PAID) {
			stockService.decreaseStock(order);
		} else if (newStatus == OrderStatus.REFUNDED) {
			stockService.restoreStock(order);
		}
		order.setStatus(newStatus);
		Order saved = getRepository().save(order);
		log.info("Venta {}: {} -> {}", saved.getId(), current, newStatus);
		return saved;
	}

	@Transactional
	public Order applyCoupon(Long id, String couponCode) {
		Order order = requireById(id);
		orderCouponService.applyCoupon(order, couponCode);
		Order saved = getRepository().save(order);
		log.info("Cupon aplicado a la venta {}: total={}", saved.getId(), saved.getTotalPrice());
		return saved;
	}

	@Transactional
	public Order removeCoupon(Long id) {
		Order order = requireById(id);
		if (order.getCoupon() == null) {
			throw new IllegalArgumentException("La venta no tiene cupon aplicado");
		}
		orderCouponService.removeCoupon(order);
		Order saved = getRepository().save(order);
		log.info("Cupon quitado de la venta {}: total={}", saved.getId(), saved.getTotalPrice());
		return saved;
	}

	public Page<Order> list(String status, Long warehouseId, String couponCode, Long bookId, LocalDateTime createdFrom,
			LocalDateTime createdTo, Integer page, Integer size, String sort) {
		Page<Order> result = getRepository().findAll(
				OrderSpecifications.build(parseStatus(status), warehouseId, couponCode, bookId, createdFrom, createdTo),
				Pageables.of(page, size, sort));
		log.debug("Listado de ventas: pagina {} con {} resultado(s)", result.getNumber(), result.getTotalElements());
		return result;
	}

	@Transactional
	public void delete(Long id) {
		Order order = requireById(id);
		order.setDeleted(true);
		getRepository().save(order);
		log.info("Venta {} eliminada (borrado logico)", id);
	}

	private void addDetail(Order order, OrderItemCreate item) {
		if (item == null || item.getBookId() == null || item.getQuantity() == null) {
			throw new IllegalArgumentException("Cada item requiere bookId y quantity");
		}
		if (item.getQuantity() <= 0) {
			throw new IllegalArgumentException("quantity debe ser mayor a 0");
		}
		Book book = bookService.requireById(item.getBookId());
		if (book.getPrice() == null) {
			throw new IllegalArgumentException("El libro " + book.getId() + " no tiene precio de venta definido");
		}

		OrderDetail detail = new OrderDetail();
		detail.setOrder(order);
		detail.setBookId(book.getId());
		detail.setBookName(book.getTitle());
		detail.setQuantity(item.getQuantity());
		detail.setUnitPrice(book.getPrice());
		detail.setDiscount(0L);
		orderDetailRepository.save(detail);
		order.getOrderDetails().add(detail);
	}

	private boolean isAllowedTransition(OrderStatus from, OrderStatus to) {
		return switch (from) {
			case PENDING -> to == OrderStatus.PAID || to == OrderStatus.CANCELLED;
			case PAID -> to == OrderStatus.REFUNDED;
			default -> false;
		};
	}

	private OrderStatus parseStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return OrderStatus.valueOf(status.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Estado invalido: " + status);
		}
	}
}
