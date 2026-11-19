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

class OrderMapperTest {

	private final OrderMapper mapper = new OrderMapper();

	@Test
	void orderDtoUsesPublicContract() {
		OrderDto dto = mapper.toDto(sampleOrder());

		assertEquals("12345678", dto.getNumeroOrden());
		assertEquals("8001234-5", dto.getInvoiceRuc());
		assertEquals("Ana", dto.getInvoiceName());
		assertEquals("Gomez", dto.getInvoiceLastName());
		assertEquals("Gabriel Garcia Marquez", dto.getShipping().getName());
		assertEquals("Asuncion", dto.getShipping().getCity());
		assertEquals(Long.valueOf(1), dto.getProductCount());
		assertEquals(Long.valueOf(2), dto.getTotalUnits());

		// El contrato publico no expone ids internos ni el orderNumber legado.
		assertNull(dto.getId());
		assertNull(dto.getOrderNumber());
		assertNull(dto.getOrderDetailIds());

		OrderDetailDto detail = dto.getOrderDetails().get(0);
		assertEquals(Long.valueOf(3), detail.getBookId());
		assertEquals("Cien anios de soledad", detail.getBookName());
		assertEquals(Integer.valueOf(2), detail.getQuantity());
		assertEquals(Long.valueOf(125000), detail.getUnitPrice());
		assertEquals(Long.valueOf(5000), detail.getDiscount());
		assertEquals(Long.valueOf(245000), detail.getLineTotal());
		assertEquals("/covers/cien.jpg", detail.getCoverSrc());
		assertEquals(List.of("Gabriel Garcia Marquez"), detail.getAuthors());
		assertNull(detail.getId());
		assertNull(detail.getOrderId());
	}

	@Test
	void detailDtoMatchesOrderArray() {
		Order order = sampleOrder();

		OrderDetailDto fromOrder = mapper.toDto(order).getOrderDetails().get(0);
		OrderDetailDto fromEndpoint = mapper.toDetailDto(order.getOrderDetails().get(0));

		assertEquals(fromOrder, fromEndpoint);
	}

	@Test
	void nullInputsReturnNull() {
		assertNull(mapper.toDto(null));
		assertNull(mapper.toDetailDto(null));
	}

	private Order sampleOrder() {
		Order order = new Order();
		order.setId(15L);
		order.setOrderNumber("PED-TEST-15");
		order.setNumeroOrden("12345678");
		order.setStatus(OrderStatus.PENDING);
		order.setInvoiceRuc("8001234-5");
		order.setInvoiceName("Ana");
		order.setInvoiceLastName("Gomez");
		order.setShippingName("Gabriel Garcia Marquez");
		order.setShippingLastName("Buendia");
		order.setShippingDepartment("Central");
		order.setShippingCity("Asuncion");
		order.setShippingAddress("Calle 1");
		order.setShippingPhone("+595991234567");

		OrderDetail detail = new OrderDetail();
		detail.setId(101L);
		detail.setBookId(3L);
		detail.setBookName("Cien anios de soledad");
		detail.setQuantity(2);
		detail.setUnitPrice(125000L);
		detail.setDiscount(5000L);
		detail.setCoverSrc("/covers/cien.jpg");
		detail.setAuthors("Gabriel Garcia Marquez");
		detail.setOrder(order);
		order.setOrderDetails(List.of(detail));
		return order;
	}
}
