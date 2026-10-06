package com.bigobooks.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.entities.stock.Stock;
import com.bigobooks.exception.ConflictException;
import com.bigobooks.repositories.StockRepository;
import com.bigobooks.service.BaseService;

/**
 * Servicio de apoyo (sin controlador): descuento y devolucion del stock al
 * cambiar el estado de una venta. Las escrituras son atomicas sobre la tabla
 * stock compartida.
 */
@Service
public class StockService extends BaseService<Stock, StockRepository> {

	private static final Logger log = LoggerFactory.getLogger(StockService.class);

	public StockService(StockRepository repository) {
		super(repository);
	}

	/**
	 * Descuenta el stock de cada detalle al confirmar el pago (PAID).
	 */
	@Transactional
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
	@Transactional
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
