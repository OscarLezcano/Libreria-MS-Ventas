package com.bigobooks.services;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import com.bigobooks.cache.CacheNames;
import com.bigobooks.dto.OrderCreateRequest;
import com.bigobooks.dto.OrderItemCreate;
import com.bigobooks.dto.OrderUpdateRequest;
import com.bigobooks.entities.auth.UserAccount;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderStatus;
import com.bigobooks.util.exception.ConflictException;
import com.bigobooks.util.exception.NotFoundException;
import com.bigobooks.util.exception.ResourceType;
import com.bigobooks.repositories.OrderRepository;
import com.bigobooks.service.BaseService;
import com.bigobooks.specifications.OrderSpecifications;

@Service
public class OrderService extends BaseService<Order, OrderRepository> {

	private static final Logger log = LoggerFactory.getLogger(OrderService.class);
	private static final Pattern NUMERO_ORDEN_PATTERN = Pattern.compile("^\\d{8}$");
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final OrderDetailService orderDetailService;
	private final UserAccountService userAccountService;
	private final WarehouseService warehouseService;
	private final StockService stockService;
	private final OrderCouponService orderCouponService;
	private final BookService bookService;
	private final OrderRequestFingerprint requestFingerprint;
	private final MailOutboxService mailOutboxService;

	public OrderService(OrderRepository repository, OrderDetailService orderDetailService,
			UserAccountService userAccountService, WarehouseService warehouseService, StockService stockService,
			OrderCouponService orderCouponService, BookService bookService,
			OrderRequestFingerprint requestFingerprint, MailOutboxService mailOutboxService) {
		super(repository);
		this.mailOutboxService = mailOutboxService;
		this.orderDetailService = orderDetailService;
		this.userAccountService = userAccountService;
		this.warehouseService = warehouseService;
		this.stockService = stockService;
		this.orderCouponService = orderCouponService;
		this.bookService = bookService;
		this.requestFingerprint = requestFingerprint;
	}

	/** Venta con sus detalles ya cargados. */
	public Order requireById(Long id) {
		return getRepository().findByIdWithDetails(id)
				.orElseThrow(() -> NotFoundException.byId(ResourceType.ORDER, id));
	}

	/** Pedido propio por numeroOrden; cualquier otro caso se trata como 404. */
	public Order requireOwnByNumeroOrden(String numeroOrden, Long userId) {
		validateNumeroOrden(numeroOrden);
		Order order = getRepository().findByNumeroOrdenWithDetails(numeroOrden)
				.orElseThrow(() -> NotFoundException.byCode(ResourceType.ORDER, numeroOrden));
		requireOwner(order, userId, numeroOrden);
		return order;
	}

	public void validateNumeroOrden(String numeroOrden) {
		if (numeroOrden == null || !NUMERO_ORDEN_PATTERN.matcher(numeroOrden).matches()) {
			throw new IllegalArgumentException("El numero de orden debe tener exactamente 8 digitos");
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public String transactionPropagationDemo(String propagation) {
		return switch (propagation.trim().toUpperCase()) {
		case "REQUIRED" -> bookService.transactionRequired();
		case "REQUIRES_NEW" -> bookService.transactionRequiresNew();
		case "SUPPORTS" -> bookService.transactionSupports();
		case "NOT_SUPPORTED" -> bookService.transactionNotSupported();
		case "NEVER" -> bookService.transactionNever();
		case "MANDATORY" -> bookService.transactionMandatory();
		default -> throw new IllegalArgumentException("Propagacion no soportada: " + propagation);
		};
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public Order createFromRequest(OrderCreateRequest request) {
		return createFromRequest(request, request != null ? request.getUserId() : null, null);
	}

	/** Crea la venta con el usuario indicado (uso interno/administrativo). */
	@Transactional(propagation = Propagation.REQUIRED)
	public Order createFromRequest(OrderCreateRequest request, Long userId) {
		return createFromRequest(request, userId, null);
	}

	/**
	 * Crea la venta. Si llega Idempotency-Key, la misma clave del mismo usuario
	 * devuelve el pedido original; reutilizarla con otro payload responde 409.
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	public Order createFromRequest(OrderCreateRequest request, Long userId, String idempotencyKey) {
		if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
			throw new IllegalArgumentException("Debe incluir al menos un item en la venta");
		}

		String key = idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim();
		String hash = requestFingerprint.of(request);
		if (key != null && userId != null) {
			Optional<Order> existing = getRepository().findByIdempotencyKeyAndUserId(key, userId);
			if (existing.isPresent()) {
				Order previous = existing.get();
				if (!hash.equals(previous.getRequestHash())) {
					throw new ConflictException("La clave de idempotencia ya fue usada con otro pedido");
				}
				return requireById(previous.getId());
			}
		}

		if (request.getWarehouseId() != null) {
			warehouseService.requireById(request.getWarehouseId());
		}
		UserAccount user = userId != null ? userAccountService.requireById(userId) : null;

		try {
			Order order = new Order();
			order.setStatus(OrderStatus.PENDING);
			order.setWarehouseId(request.getWarehouseId());
			order.setUserAccount(user);
			order.setNumeroOrden(generateNumeroOrden());
			order.setIdempotencyKey(key);
			order.setRequestHash(hash);
			validateInvoiceData(request);
			validateShippingData(request.getShippingName(), request.getShippingLastName(),
					request.getShippingDepartment(), request.getShippingCity(), request.getShippingAddress(),
					request.getShippingReference(), request.getShippingPhone(), true);
			order.setInvoiceName(normalizeText(request.getInvoiceName()));
			order.setInvoiceLastName(normalizeText(request.getInvoiceLastName()));
			order.setInvoiceRuc(normalizeDocument(request.getInvoiceRuc()));
			order.setInvoicePhone(normalizePhone(request.getInvoicePhone()));
			applyShipping(order, request.getShippingName(), request.getShippingLastName(),
					request.getShippingDepartment(), request.getShippingCity(), request.getShippingAddress(),
					request.getShippingReference(), request.getShippingPhone());
			order.setOrderDetails(new ArrayList<>());
			Order saved = getRepository().save(order);

			for (OrderItemCreate item : request.getItems()) {
				orderDetailService.addDetailTo(saved, item);
			}
			orderCouponService.recalculate(saved);

			if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
				orderCouponService.applyCoupon(saved, request.getCouponCode());
			}

			// Valida stock tambien cuando el almacen viene explicito en el request.
			saved.setWarehouseId(stockService.resolveWarehouseId(saved));
			Order result = getRepository().save(saved);
			if (result.getUserAccount() != null) {
				mailOutboxService.enqueue(result.getUserAccount().getEmail(),
						"Compra registrada " + result.getNumeroOrden(),
						"Hola " + result.getUserAccount().getName() + ",\n\nTu compra fue registrada con el numero de orden "
								+ result.getNumeroOrden() + " por un total de " + result.getTotalPrice() + " Gs.\n\nGracias por tu compra.");
			}
			log.info("Venta {} creada: numeroOrden={}, {} item(s), subtotal={}, descuento={}, total={}",
					result.getId(), result.getNumeroOrden(), result.getOrderDetails().size(), result.getSubtotal(),
					result.getDiscountAmount(), result.getTotalPrice());
			return result;
		} catch (DataIntegrityViolationException exception) {
			throw new ConflictException("No se pudo crear el pedido; intentelo nuevamente");
		}
	}

	@CacheEvict(cacheNames = CacheNames.ORDERS, key = "#id")
	@Transactional(propagation = Propagation.REQUIRED)
	public Order update(Long id, OrderUpdateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Debe indicar los datos de la venta");
		}
		Order order = requireById(id);
		if (order.getStatus() != OrderStatus.PENDING) {
			throw new ConflictException("Solo se puede editar una venta en estado PENDING");
		}
		validateInvoiceUpdate(request);
		validateShippingData(request.getShippingName(), request.getShippingLastName(),
				request.getShippingDepartment(), request.getShippingCity(), request.getShippingAddress(),
				request.getShippingReference(), request.getShippingPhone(), false);
		if (request.getWarehouseId() != null) {
			warehouseService.requireById(request.getWarehouseId());
			order.setWarehouseId(request.getWarehouseId());
		}
		if (request.getUserId() != null) {
			order.setUserAccount(userAccountService.requireById(request.getUserId()));
		}
		if (request.getInvoiceNumber() != null) {
			order.setInvoiceNumber(request.getInvoiceNumber());
		}
		if (request.getInvoiceRuc() != null) {
			order.setInvoiceRuc(normalizeDocument(request.getInvoiceRuc()));
		}
		if (request.getInvoiceName() != null) {
			order.setInvoiceName(normalizeText(request.getInvoiceName()));
		}
		if (request.getInvoiceLastName() != null) {
			order.setInvoiceLastName(normalizeText(request.getInvoiceLastName()));
		}
		if (request.getInvoicePhone() != null) {
			order.setInvoicePhone(normalizePhone(request.getInvoicePhone()));
		}
		applyShippingUpdate(order, request);
		Order saved = getRepository().save(order);
		log.info("Venta {} actualizada", saved.getId());
		return saved;
	}

	/**
	 * Transiciones administrativas: PENDING-&gt;PAID, PENDING-&gt;CANCELLED y
	 * PAID-&gt;REFUNDED. El pago descuenta stock y el reembolso lo devuelve.
	 */
	@CacheEvict(cacheNames = CacheNames.ORDERS, key = "#id")
	@Transactional(propagation = Propagation.REQUIRED)
	public Order changeStatus(Long id, OrderStatus newStatus) {
		if (newStatus == null) {
			throw new IllegalArgumentException("status es obligatorio");
		}
		return applyStatusChange(requireById(id), newStatus);
	}

	/** Confirmacion/cancelacion publica por numeroOrden, con bloqueo de fila. */
	@Transactional(propagation = Propagation.REQUIRED)
	public Order changeOwnStatus(String numeroOrden, Long userId, OrderStatus newStatus) {
		validateNumeroOrden(numeroOrden);
		if (newStatus != OrderStatus.PAID && newStatus != OrderStatus.CANCELLED) {
			throw new IllegalArgumentException("El cliente solo puede pagar o cancelar su pedido");
		}
		Order order = getRepository().findByNumeroOrdenForUpdate(numeroOrden)
				.orElseThrow(() -> NotFoundException.byCode(ResourceType.ORDER, numeroOrden));
		requireOwner(order, userId, numeroOrden);
		return applyStatusChange(order, newStatus);
	}

	private Order applyStatusChange(Order order, OrderStatus newStatus) {
		OrderStatus current = order.getStatus();
		if (current == newStatus) {
			if (newStatus == OrderStatus.PAID) {
				return order;
			}
			throw new ConflictException("La venta ya esta en el estado " + current);
		}
		if (!isAllowedTransition(current, newStatus)) {
			throw new ConflictException("Transicion no permitida: " + current + " -> " + newStatus);
		}
		if (newStatus == OrderStatus.PAID) {
			if (order.getWarehouseId() == null) {
				order.setWarehouseId(stockService.resolveWarehouseId(order));
			}
			stockService.decreaseStock(order);
			if (isBlank(order.getInvoiceNumber())) {
				order.setInvoiceNumber(formatInvoiceNumber(order.getId()));
			}
		} else if (newStatus == OrderStatus.REFUNDED) {
			stockService.restoreStock(order);
		}
		order.setStatus(newStatus);
		Order saved = getRepository().save(order);
		log.info("Venta {}: {} -> {}", saved.getId(), current, newStatus);
		return requireById(saved.getId());
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Page<Order> list(String status, Long userId, Long warehouseId, String couponCode, Long bookId,
			LocalDateTime createdFrom, LocalDateTime createdTo, Integer page, Integer size, String sort) {
		return list(status, userId, warehouseId, couponCode, bookId, createdFrom, createdTo, page, size, sort, null);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Page<Order> list(String status, Long userId, Long warehouseId, String couponCode, Long bookId,
			LocalDateTime createdFrom, LocalDateTime createdTo, Integer page, Integer size, String sort,
			String search) {
		return list(status, userId, warehouseId, couponCode, bookId, createdFrom, createdTo, page, size, sort, search,
				null, null);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Page<Order> list(String status, Long userId, Long warehouseId, String couponCode, Long bookId,
			LocalDateTime createdFrom, LocalDateTime createdTo, Integer page, Integer size, String sort,
			String search, Long minTotal, Long maxTotal) {
		if ((minTotal != null && minTotal < 0) || (maxTotal != null && maxTotal < 0)) {
			throw new IllegalArgumentException("Los totales del filtro no pueden ser negativos");
		}
		if (minTotal != null && maxTotal != null && minTotal > maxTotal) {
			throw new IllegalArgumentException("El total minimo no puede ser mayor que el total maximo");
		}
		Page<Order> result = getRepository().findAll(
				OrderSpecifications.build(parseStatus(status), userId, warehouseId, couponCode, bookId, createdFrom,
						createdTo, search, minTotal, maxTotal),
				Pageables.of(page, size, sort));
		// La paginacion no usa fetch join de la coleccion para no paginar en memoria;
		// se inicializa aqui, dentro de la transaccion REQUIRED, antes de mapear el DTO.
		// SUPPORTS no sirve: el repository cierra su EntityManager al terminar findAll.
		for (Order order : result.getContent()) {
			if (order.getOrderDetails() != null) {
				order.getOrderDetails().size();
			}
		}
		log.debug("Listado de ventas: pagina {} con {} resultado(s)", result.getNumber(), result.getTotalElements());
		return result;
	}

	/** Ventas de un usuario puntual (usado por "Mis ordenes"). */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Page<Order> listByUser(Long userId, String status, LocalDateTime createdFrom, LocalDateTime createdTo,
			Integer page, Integer size, String sort) {
		return listByUser(userId, status, createdFrom, createdTo, page, size, sort, null);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Page<Order> listByUser(Long userId, String status, LocalDateTime createdFrom, LocalDateTime createdTo,
			Integer page, Integer size, String sort, String search) {
		return listByUser(userId, status, createdFrom, createdTo, page, size, sort, search, null, null);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Page<Order> listByUser(Long userId, String status, LocalDateTime createdFrom, LocalDateTime createdTo,
			Integer page, Integer size, String sort, String search, Long minTotal, Long maxTotal) {
		return list(status, userId, null, null, null, createdFrom, createdTo, page, size, sort, search, minTotal,
				maxTotal);
	}

	@CacheEvict(cacheNames = CacheNames.ORDERS, key = "#id")
	@Transactional(propagation = Propagation.REQUIRED)
	public void delete(Long id) {
		Order order = requireById(id);
		order.setDeleted(true);
		getRepository().save(order);
		log.info("Venta {} eliminada (borrado logico)", id);
	}

	private void requireOwner(Order order, Long userId, String requestedNumeroOrden) {
		if (userId == null || order.getUserAccount() == null
				|| !userId.equals(order.getUserAccount().getId())) {
			throw NotFoundException.byCode(ResourceType.ORDER, requestedNumeroOrden);
		}
	}

	private String generateNumeroOrden() {
		for (int attempt = 0; attempt < 20; attempt++) {
			String candidate = String.valueOf(SECURE_RANDOM.nextInt(90000000) + 10000000);
			if (!getRepository().existsByNumeroOrden(candidate)) {
				return candidate;
			}
		}
		throw new ConflictException("No se pudo generar un numero de orden unico");
	}

	private boolean isAllowedTransition(OrderStatus from, OrderStatus to) {
		return switch (from) {
			case PENDING -> to == OrderStatus.PAID || to == OrderStatus.CANCELLED;
			case PAID -> to == OrderStatus.REFUNDED;
			default -> false;
		};
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
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

	private void validateInvoiceData(OrderCreateRequest request) {
		if (isBlank(request.getInvoiceName()) || isBlank(request.getInvoiceLastName())
				|| isBlank(request.getInvoiceRuc()) || isBlank(request.getInvoicePhone())) {
			throw new IllegalArgumentException("La factura requiere RUC, nombre, apellido y telefono");
		}
		validateInvoiceDocument(request.getInvoiceRuc());
		normalizePhone(request.getInvoicePhone());
	}

	private void validateInvoiceUpdate(OrderUpdateRequest request) {
		if (request.getInvoiceName() != null && isBlank(request.getInvoiceName())) {
			throw new IllegalArgumentException("El nombre de factura no puede estar vacio");
		}
		if (request.getInvoiceLastName() != null && isBlank(request.getInvoiceLastName())) {
			throw new IllegalArgumentException("El apellido de factura no puede estar vacio");
		}
		if (request.getInvoiceRuc() != null) {
			validateInvoiceDocument(request.getInvoiceRuc());
		}
		if (request.getInvoicePhone() != null) {
			normalizePhone(request.getInvoicePhone());
		}
	}

	private void validateInvoiceDocument(String value) {
		if (isBlank(value) || !value.trim().matches("[0-9]{5,15}(-[0-9Kk])?")) {
			throw new IllegalArgumentException("El RUC o CI no tiene un formato valido");
		}
	}

	private void validateShippingData(String name, String lastName, String department, String city, String address,
			String reference, String phone, boolean required) {
		if (required && (isBlank(name) || isBlank(lastName) || isBlank(department) || isBlank(city)
				|| isBlank(address) || isBlank(phone))) {
			throw new IllegalArgumentException("El envio requiere nombre, apellido, departamento, ciudad, direccion y telefono");
		}
		if (name != null && isBlank(name) || lastName != null && isBlank(lastName)
				|| department != null && isBlank(department) || city != null && isBlank(city)
				|| address != null && isBlank(address) || phone != null && isBlank(phone)) {
			throw new IllegalArgumentException("Los datos de envio no pueden estar vacios");
		}
		normalizePhone(phone);
	}

	private void applyShipping(Order order, String name, String lastName, String department, String city,
			String address, String reference, String phone) {
		order.setShippingName(normalizeText(name));
		order.setShippingLastName(normalizeText(lastName));
		order.setShippingDepartment(normalizeText(department));
		order.setShippingCity(normalizeText(city));
		order.setShippingAddress(normalizeText(address));
		order.setShippingReference(isBlank(reference) ? null : normalizeText(reference));
		order.setShippingPhone(normalizePhone(phone));
	}

	private void applyShippingUpdate(Order order, OrderUpdateRequest request) {
		if (request.getShippingName() != null) {
			order.setShippingName(normalizeText(request.getShippingName()));
		}
		if (request.getShippingLastName() != null) {
			order.setShippingLastName(normalizeText(request.getShippingLastName()));
		}
		if (request.getShippingDepartment() != null) {
			order.setShippingDepartment(normalizeText(request.getShippingDepartment()));
		}
		if (request.getShippingCity() != null) {
			order.setShippingCity(normalizeText(request.getShippingCity()));
		}
		if (request.getShippingAddress() != null) {
			order.setShippingAddress(normalizeText(request.getShippingAddress()));
		}
		if (request.getShippingReference() != null) {
			order.setShippingReference(isBlank(request.getShippingReference()) ? null
					: normalizeText(request.getShippingReference()));
		}
		if (request.getShippingPhone() != null) {
			order.setShippingPhone(normalizePhone(request.getShippingPhone()));
		}
	}

	private String formatInvoiceNumber(Long orderId) {
		return String.format("001-001-%08d", orderId);
	}

	private String normalizeText(String value) {
		return value.trim().replaceAll("\\s+", " ");
	}

	private String normalizeDocument(String value) {
		return value.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
	}

	private String normalizePhone(String value) {
		if (value == null) {
			return null;
		}
		String compact = value.trim().replaceAll("[\\s().-]", "");
		if (compact.matches("09[0-9]{8}")) {
			return "+595" + compact.substring(1);
		}
		if (compact.matches("\\+5959[0-9]{8}")) {
			return compact;
		}
		throw new IllegalArgumentException("El telefono debe tener formato 0999999999 o +595 999999999");
	}
}
