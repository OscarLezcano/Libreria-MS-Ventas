package com.bigobooks.mappers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bigobooks.dto.OrderDetailDto;
import com.bigobooks.dto.OrderDto;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;

/**
 * Conversion bidireccional entre Order/OrderDetail y sus DTOs del common.
 */
@Component
public class OrderMapper {

	public OrderDto toDto(Order order) {
		if (order == null) {
			return null;
		}
		OrderDto dto = new OrderDto();
		dto.setId(order.getId());
		dto.setWarehouseId(order.getWarehouseId());
		dto.setUserId(order.getUserAccount() != null ? order.getUserAccount().getId() : null);
		dto.setStatus(order.getStatus() != null ? OrderDto.StatusEnum.valueOf(order.getStatus().name()) : null);
		dto.setTotalPrice(order.getTotalPrice());
		dto.setSubtotal(order.getSubtotal());
		dto.setDiscountAmount(order.getDiscountAmount());
		dto.setBancardNumber(order.getBancardNumber());
		dto.setInvoiceNumber(order.getInvoiceNumber());
		dto.setCreatedAt(order.getCreatedAt());
		dto.setCouponId(order.getCoupon() != null ? order.getCoupon().getId() : null);

		List<OrderDetailDto> details = new ArrayList<>();
		if (order.getOrderDetails() != null) {
			for (OrderDetail detail : order.getOrderDetails()) {
				details.add(toDetailDto(detail));
			}
		}
		dto.setOrderDetails(details);
		return dto;
	}

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
