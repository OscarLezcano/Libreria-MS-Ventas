package com.bigobooks.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.entities.stock.Stock;
import com.bigobooks.entities.stock.Warehouse;
import com.bigobooks.util.exception.ConflictException;
import com.bigobooks.repositories.StockRepository;
import com.bigobooks.repositories.WarehouseRepository;
import com.bigobooks.service.BaseService;

/**
 * Servicio de apoyo (sin controlador): descuento y devolucion del stock al
 * cambiar el estado de una venta. Las escrituras son atomicas sobre la tabla
 * stock compartida.
 */
@Service
public class StockService extends BaseService<Stock, StockRepository> {

	private static final Logger log = LoggerFactory.getLogger(StockService.class);

	private final WarehouseRepository warehouseRepository;

	public StockService(StockRepository repository, WarehouseRepository warehouseRepository) {
		super(repository);
		this.warehouseRepository = warehouseRepository;
	}

	/**
	 * Selecciona una sola sucursal que pueda atender todos los items del pedido.
	 * Se usa cuando el cliente no conoce ni debe enviar un ID interno de almacen.
	 */
	public Long resolveWarehouseId(Order order) {
		if (order.getWarehouseId() != null) {
			return order.getWarehouseId();
		}

		Map<Long, Integer> requiredQuantities = new HashMap<>();
		for (OrderDetail detail : order.getOrderDetails()) {
			requiredQuantities.merge(detail.getBookId(), detail.getQuantity(), Integer::sum);
		}
		List<Warehouse> warehouses = warehouseRepository.findAll();
		for (Warehouse warehouse : warehouses) {
			boolean hasAllItems = requiredQuantities.entrySet().stream()
					.allMatch(entry -> hasSufficientStock(entry.getKey(), entry.getValue(), warehouse.getId()));
			if (hasAllItems) {
				log.info("Almacen {} seleccionado automaticamente para la venta {}", warehouse.getId(),
						order.getId());
				return warehouse.getId();
			}
		}

		throw new ConflictException("No hay un almacen con stock suficiente para todos los libros de la venta");
	}

	private boolean hasSufficientStock(Long bookId, int quantity, Long warehouseId) {
		return getRepository().findByBookIdAndWarehouse_Id(bookId, warehouseId)
				.map(stock -> stock.getQuantity() >= quantity)
				.orElse(false);
	}

	/**
	 * Descuenta el stock de cada detalle al confirmar el pago (PAID).
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void decreaseStock(Order order) {
		Long warehouseId = order.getWarehouseId();
		if (warehouseId == null) {
			throw new IllegalArgumentException("La venta debe indicar un almacen antes de confirmar el pago");
		}
		for (OrderDetail detail : order.getOrderDetails()) {
			Stock stock = requireStockFor(detail, warehouseId);
			int updated = getRepository().decreaseQuantity(stock.getId(), detail.getQuantity());
			if (updated == 0) {
				throw new ConflictException("Stock insuficiente del libro " + detail.getBookId() + " en el almacen "
						+ warehouseId);
			}
			log.debug("Stock libro {} almacen {}: -{}", detail.getBookId(), warehouseId, detail.getQuantity());
		}
		log.info("Stock descontado para la venta {} en el almacen {}", order.getId(), warehouseId);
	}

	/**
	 * Devuelve el stock de cada detalle al reembolsar (REFUNDED).
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void restoreStock(Order order) {
		Long warehouseId = order.getWarehouseId();
		if (warehouseId == null) {
			throw new IllegalArgumentException("La venta debe indicar un almacen para reversar el stock");
		}
		for (OrderDetail detail : order.getOrderDetails()) {
			Stock stock = requireStockFor(detail, warehouseId);
			getRepository().increaseQuantity(stock.getId(), detail.getQuantity());
			log.debug("Stock libro {} almacen {}: +{}", detail.getBookId(), warehouseId, detail.getQuantity());
		}
		log.info("Stock devuelto para la venta {} en el almacen {}", order.getId(), warehouseId);
	}

	private Stock requireStockFor(OrderDetail detail, Long warehouseId) {
		return getRepository().findByBookIdAndWarehouse_Id(detail.getBookId(), warehouseId)
				.orElseThrow(() -> new ConflictException("No hay stock registrado del libro " + detail.getBookId()
						+ " en el almacen " + warehouseId));
	}
}
