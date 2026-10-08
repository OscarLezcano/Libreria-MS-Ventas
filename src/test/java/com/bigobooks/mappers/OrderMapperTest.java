package com.bigobooks.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.bigobooks.dto.OrderDetailDto;
import com.bigobooks.dto.OrderDto;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.entities.orders.OrderStatus;

/**
 * Conversion bidireccional Order/OrderDetail &lt;-&gt; DTO (Entrega #2).
 */
class OrderMapperTest {

	private final OrderMapper mapper = new OrderMapper();

	@Test
	void convierteEntidadADto() {
		Order order = new Order();
		order.setId(1L);
		order.setWarehouseId(5L);
		order.setStatus(OrderStatus.PENDING);
		order.setSubtotal(2000L);
		order.setDiscountAmount(0L);
		order.setTotalPrice(2000L);
		order.setInvoiceNumber("F-001");

		OrderDetail detail = new OrderDetail();
		detail.setId(3L);
		detail.setBookId(10L);
		detail.setBookName("El principito");
		detail.setQuantity(2);
		detail.setUnitPrice(1000L);
		detail.setOrder(order);
		order.setOrderDetails(List.of(detail));

		OrderDto dto = mapper.toDto(order);

		assertEquals(1L, dto.getId());
		assertEquals(5L, dto.getWarehouseId());
		assertEquals(OrderDto.StatusEnum.PENDING, dto.getStatus());
		assertEquals(2000L, dto.getSubtotal());
		assertEquals(2000L, dto.getTotalPrice());
		assertEquals("F-001", dto.getInvoiceNumber());
		assertEquals(1, dto.getOrderDetails().size());
		assertEquals(3L, dto.getOrderDetails().get(0).getId());
		assertEquals("El principito", dto.getOrderDetails().get(0).getBookName());
	}

	@Test
	void convierteDtoAEntidad() {
		OrderDetailDto detailDto = new OrderDetailDto();
		detailDto.setId(3L);
		detailDto.setBookId(10L);
		detailDto.setBookName("El principito");
		detailDto.setQuantity(2);
		detailDto.setUnitPrice(1000L);
		detailDto.setDiscount(0L);

		OrderDto dto = new OrderDto();
		dto.setId(1L);
		dto.setWarehouseId(5L);
		dto.setStatus(OrderDto.StatusEnum.PENDING);
		dto.setSubtotal(2000L);
		dto.setDiscountAmount(0L);
		dto.setTotalPrice(2000L);
		dto.setInvoiceNumber("F-001");
		dto.setOrderDetails(List.of(detailDto));

		Order order = mapper.toEntity(dto);

		assertEquals(1L, order.getId());
		assertEquals(5L, order.getWarehouseId());
		assertEquals(OrderStatus.PENDING, order.getStatus());
		assertEquals(2000L, order.getSubtotal());
		assertEquals(2000L, order.getTotalPrice());
		assertEquals("F-001", order.getInvoiceNumber());
		assertEquals(1, order.getOrderDetails().size());
		assertEquals(order, order.getOrderDetails().get(0).getOrder());
		assertEquals("El principito", order.getOrderDetails().get(0).getBookName());
	}

	@Test
	void elRoundtripConservaLosCamposPlanos() {
		Order order = new Order();
		order.setId(9L);
		order.setStatus(OrderStatus.PAID);
		order.setSubtotal(1500L);
		order.setDiscountAmount(150L);
		order.setTotalPrice(1350L);

		OrderDto dto = mapper.toDto(mapper.toEntity(mapper.toDto(order)));

		assertEquals(order.getId(), dto.getId());
		assertEquals(order.getStatus(), OrderStatus.valueOf(dto.getStatus().name()));
		assertEquals(order.getSubtotal(), dto.getSubtotal());
		assertEquals(order.getDiscountAmount(), dto.getDiscountAmount());
		assertEquals(order.getTotalPrice(), dto.getTotalPrice());
	}

	@Test
	void convierteValoresNulos() {
		assertNull(mapper.toDto(null));
		assertNull(mapper.toEntity((OrderDto) null));
		assertNull(mapper.toDetailDto(null));
		assertNull(mapper.toEntity((OrderDetailDto) null));
	}
}
