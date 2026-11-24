package com.bigobooks.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.entities.stock.Stock;
import com.bigobooks.entities.stock.Warehouse;
import com.bigobooks.repositories.StockRepository;
import com.bigobooks.repositories.WarehouseRepository;
import com.bigobooks.util.exception.ConflictException;

class StockServiceTest {

	private final StockRepository stockRepository = Mockito.mock(StockRepository.class);
	private final WarehouseRepository warehouseRepository = Mockito.mock(WarehouseRepository.class);
	private final StockService stockService = new StockService(stockRepository, warehouseRepository);

	@Test
	void resolveWarehouseSelectsWarehouseWithStockForEveryItem() {
		Warehouse warehouse = warehouse(10L);
		Order order = orderWithItems(item(1L, 2), item(2L, 1));

		when(warehouseRepository.findAll()).thenReturn(List.of(warehouse));
		when(stockRepository.findByBookIdAndWarehouse_Id(1L, 10L)).thenReturn(Optional.of(stock(2)));
		when(stockRepository.findByBookIdAndWarehouse_Id(2L, 10L)).thenReturn(Optional.of(stock(1)));

		assertEquals(10L, stockService.resolveWarehouseId(order));
	}

	@Test
	void resolveWarehouseRejectsWhenNoWarehouseCanFulfilTheWholeOrder() {
		Warehouse warehouse = warehouse(10L);
		Order order = orderWithItems(item(1L, 2), item(2L, 1));

		when(warehouseRepository.findAll()).thenReturn(List.of(warehouse));
		when(stockRepository.findByBookIdAndWarehouse_Id(1L, 10L)).thenReturn(Optional.of(stock(2)));
		when(stockRepository.findByBookIdAndWarehouse_Id(2L, 10L)).thenReturn(Optional.of(stock(0)));

		assertThrows(ConflictException.class, () -> stockService.resolveWarehouseId(order));
	}

	private static Order orderWithItems(OrderDetail... items) {
		Order order = new Order();
		order.setOrderDetails(List.of(items));
		return order;
	}

	private static OrderDetail item(Long bookId, int quantity) {
		OrderDetail detail = new OrderDetail();
		detail.setBookId(bookId);
		detail.setQuantity(quantity);
		return detail;
	}

	private static Warehouse warehouse(Long id) {
		Warehouse warehouse = new Warehouse();
		warehouse.setId(id);
		return warehouse;
	}

	private static Stock stock(int quantity) {
		Stock stock = new Stock();
		stock.setQuantity(quantity);
		return stock;
	}
}
