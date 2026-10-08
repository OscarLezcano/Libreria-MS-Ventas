package com.bigobooks.services;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigobooks.dto.OrderItemCreate;
import com.bigobooks.entities.book.Book;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.repositories.OrderDetailRepository;
import com.bigobooks.service.BaseService;

/**
 * Servicio de apoyo (sin controlador): gestiona los detalles (libros) de una
 * venta. De esta forma OrderService trabaja exclusivamente sobre la entidad
 * Order, cumpliendo la especializacion por entidad.
 */
@Service
public class OrderDetailService extends BaseService<OrderDetail, OrderDetailRepository> {

	private static final Logger log = LoggerFactory.getLogger(OrderDetailService.class);

	private final BookService bookService;

	public OrderDetailService(OrderDetailRepository repository, BookService bookService) {
		super(repository);
		this.bookService = bookService;
	}

	/**
	 * Valida el item del request, resuelve el libro, guarda el detalle y lo
	 * vincula a la venta.
	 */
	@Transactional
	public OrderDetail addDetailTo(Order order, OrderItemCreate item) {
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
		getRepository().save(detail);
		if (order.getOrderDetails() == null) {
			order.setOrderDetails(new ArrayList<>());
		}
		order.getOrderDetails().add(detail);
		log.debug("Detalle del libro {} agregado a la venta {} ({} u.)", detail.getBookId(), order.getId(),
				detail.getQuantity());
		return detail;
	}
}
