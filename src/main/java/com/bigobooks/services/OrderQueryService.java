package com.bigobooks.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bigobooks.cache.CacheNames;
import com.bigobooks.dto.BestSellingBookDto;
import com.bigobooks.dto.OrderDetailDto;
import com.bigobooks.dto.OrderDto;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.entities.orders.OrderStatus;
import com.bigobooks.mappers.OrderMapper;
import com.bigobooks.repositories.OrderDetailRepository;
import com.bigobooks.repositories.OrderDetailRepository.BestSellingBookProjection;

/**
 * Lecturas cacheadas de ventas. Se cachea el DTO y nunca la entidad JPA, para
 * no guardar proxies lazy ni relaciones no inicializadas en Redis.
 */
@Service
public class OrderQueryService {

	private static final int DEFAULT_BEST_SELLERS = 10;

	private static final int MAX_BEST_SELLERS = 100;

	private final OrderService orderService;

	private final OrderMapper orderMapper;

	private final OrderDetailRepository orderDetailRepository;

	public OrderQueryService(OrderService orderService, OrderMapper orderMapper,
			OrderDetailRepository orderDetailRepository) {
		this.orderService = orderService;
		this.orderMapper = orderMapper;
		this.orderDetailRepository = orderDetailRepository;
	}

	/** Venta con los ids de sus detalles. */
	@Cacheable(cacheNames = CacheNames.ORDERS, key = "#id", sync = true)
	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public OrderDto getById(Long id) {
		return orderMapper.toDto(orderService.requireById(id));
	}

	/**
	 * Desglose completo de la venta. No se cachea para no mezclar tipos en el
	 * cache de ventas.
	 */
	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public List<OrderDetailDto> getDetails(Long id) {
		Order order = orderService.requireById(id);
		List<OrderDetailDto> details = new ArrayList<>();
		if (order.getOrderDetails() != null) {
			for (OrderDetail detail : order.getOrderDetails()) {
				details.add(orderMapper.toDetailDto(detail));
			}
		}
		return details;
	}

	/**
	 * Libros mas vendidos: suma las unidades de los detalles de ventas PAID.
	 * El limite se acota entre 1 y 100.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
	public List<BestSellingBookDto> getBestSellingBooks(Integer limit) {
		int size = limit == null ? DEFAULT_BEST_SELLERS : limit;
		size = Math.max(1, Math.min(size, MAX_BEST_SELLERS));
		List<BestSellingBookProjection> rows = orderDetailRepository.findBestSellingBooks(OrderStatus.PAID,
				PageRequest.of(0, size));
		List<BestSellingBookDto> books = new ArrayList<>(rows.size());
		for (BestSellingBookProjection row : rows) {
			BestSellingBookDto dto = new BestSellingBookDto();
			dto.setBookId(row.getBookId());
			dto.setBookName(row.getBookName());
			dto.setTotalSold(row.getTotalSold());
			books.add(dto);
		}
		return books;
	}
}
