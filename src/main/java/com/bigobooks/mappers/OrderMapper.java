package com.bigobooks.mappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bigobooks.dto.OrderDetailDto;
import com.bigobooks.dto.OrderDto;
import com.bigobooks.dto.OrderShippingDto;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.services.OrderCouponService;

/**
 * Conversion de Order/OrderDetail a los DTOs publicos. No se exponen los ids
 * internos ni el orderNumber legado: el contrato publico usa numeroOrden.
 */
@Component
public class OrderMapper {

	/** DTO publico de la venta, con el desglose completo y el snapshot de envio. */
	public OrderDto toDto(Order order) {
		if (order == null) {
			return null;
		}
		OrderDto dto = new OrderDto();
		dto.setWarehouseId(order.getWarehouseId());
		dto.setUserId(order.getUserAccount() != null ? order.getUserAccount().getId() : null);
		dto.setStatus(order.getStatus() != null ? OrderDto.StatusEnum.valueOf(order.getStatus().name()) : null);
		dto.setNumeroOrden(order.getNumeroOrden());
		dto.setTotalPrice(order.getTotalPrice());
		dto.setSubtotal(order.getSubtotal());
		dto.setDiscountAmount(order.getDiscountAmount());
		dto.setCouponCode(order.getCouponCode());
		dto.setInvoiceNumber(order.getInvoiceNumber());
		dto.setInvoiceRuc(order.getInvoiceRuc());
		dto.setInvoiceName(order.getInvoiceName());
		dto.setInvoiceLastName(order.getInvoiceLastName());
		dto.setInvoicePhone(order.getInvoicePhone());
		dto.setShipping(toShipping(order));
		dto.setCreatedAt(order.getCreatedAt());
		dto.setCouponId(order.getCoupon() != null ? order.getCoupon().getId() : null);

		List<OrderDetailDto> details = new ArrayList<>();
		long totalUnits = 0;
		if (order.getOrderDetails() != null) {
			for (OrderDetail detail : order.getOrderDetails()) {
				OrderDetailDto detailDto = toDetailDto(detail);
				if (detailDto != null) {
					details.add(detailDto);
					totalUnits += detailDto.getQuantity() == null ? 0 : detailDto.getQuantity().longValue();
				}
			}
		}
		dto.setOrderDetails(details);
		dto.setProductCount((long) details.size());
		dto.setTotalUnits(totalUnits);
		// Campos legados de uso interno: se dejan nulos para que no se serialicen.
		dto.setId(null);
		dto.setOrderNumber(null);
		dto.setOrderDetailIds(null);
		return dto;
	}

	/** DTO publico de un detalle (mismo contenido en el pedido y en /details). */
	public OrderDetailDto toDetailDto(OrderDetail detail) {
		if (detail == null) {
			return null;
		}
		OrderDetailDto dto = new OrderDetailDto();
		dto.setBookId(detail.getBookId());
		dto.setBookName(detail.getBookName());
		dto.setQuantity(detail.getQuantity());
		dto.setUnitPrice(detail.getUnitPrice());
		dto.setDiscount(detail.getDiscount());
		dto.setLineTotal(OrderCouponService.calculateLineTotal(detail));
		dto.setCoverSrc(detail.getCoverSrc());
		dto.setAuthors(toAuthors(detail.getAuthors()));
		// ids internos y createdAt no forman parte del contrato publico.
		dto.setId(null);
		dto.setOrderId(null);
		dto.setCreatedAt(null);
		return dto;
	}

	private OrderShippingDto toShipping(Order order) {
		if (order.getShippingName() == null && order.getShippingLastName() == null
				&& order.getShippingDepartment() == null && order.getShippingCity() == null
				&& order.getShippingAddress() == null && order.getShippingReference() == null
				&& order.getShippingPhone() == null) {
			return null;
		}
		OrderShippingDto shipping = new OrderShippingDto();
		shipping.setName(order.getShippingName());
		shipping.setLastName(order.getShippingLastName());
		shipping.setDepartment(order.getShippingDepartment());
		shipping.setCity(order.getShippingCity());
		shipping.setAddress(order.getShippingAddress());
		shipping.setReference(order.getShippingReference());
		shipping.setPhone(order.getShippingPhone());
		return shipping;
	}

	private List<String> toAuthors(String authors) {
		if (authors == null || authors.isBlank()) {
			return List.of();
		}
		return Arrays.stream(authors.split(","))
				.map(String::trim)
				.filter(name -> !name.isEmpty())
				.toList();
	}
}
