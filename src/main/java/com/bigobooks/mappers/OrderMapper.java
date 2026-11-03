package com.bigobooks.mappers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bigobooks.dto.OrderDetailDto;
import com.bigobooks.dto.OrderDto;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;

/**
 * Conversion de Order/OrderDetail a sus DTOs del common. La venta solo lleva
 * los ids de sus detalles; el desglose completo se expone por separado.
 */
@Component
public class OrderMapper {

	/** DTO de venta con los ids de sus detalles (sin anidar objetos). */
	public OrderDto toDto(Order order) {
		if (order == null) {
			return null;
		}
		OrderDto dto = new OrderDto();
		dto.setId(order.getId());
		dto.setOrderNumber(order.getOrderNumber());
		dto.setWarehouseId(order.getWarehouseId());
		dto.setUserId(order.getUserAccount() != null ? order.getUserAccount().getId() : null);
		dto.setStatus(order.getStatus() != null ? OrderDto.StatusEnum.valueOf(order.getStatus().name()) : null);
		dto.setTotalPrice(order.getTotalPrice());
		dto.setSubtotal(order.getSubtotal());
		dto.setDiscountAmount(order.getDiscountAmount());
		dto.setBancardNumber(order.getBancardNumber());
		dto.setInvoiceNumber(order.getInvoiceNumber());
		dto.setInvoiceBusinessName(order.getInvoiceBusinessName());
		dto.setInvoiceName(order.getInvoiceName());
		dto.setInvoiceLastName(order.getInvoiceLastName());
		dto.setCreatedAt(order.getCreatedAt());
		dto.setCouponId(order.getCoupon() != null ? order.getCoupon().getId() : null);

		List<Long> detailIds = new ArrayList<>();
		if (order.getOrderDetails() != null) {
			for (OrderDetail detail : order.getOrderDetails()) {
				if (detail != null && detail.getId() != null) {
					detailIds.add(detail.getId());
				}
			}
		}
		dto.setOrderDetailIds(detailIds);
		return dto;
	}

	/** DTO completo de un detalle (se usa en GET /orders/{id}/details). */
	public OrderDetailDto toDetailDto(OrderDetail detail) {
		if (detail == null) {
			return null;
		}
		OrderDetailDto dto = new OrderDetailDto();
		dto.setId(detail.getId());
		dto.setBookId(detail.getBookId());
		dto.setBookName(detail.getBookName());
		dto.setQuantity(detail.getQuantity());
		dto.setUnitPrice(detail.getUnitPrice());
		dto.setDiscount(detail.getDiscount());
		dto.setCreatedAt(detail.getCreatedAt());
		dto.setOrderId(detail.getOrder() != null ? detail.getOrder().getId() : null);
		return dto;
	}
}
