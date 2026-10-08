package com.bigobooks.mappers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bigobooks.dto.OrderDetailDto;
import com.bigobooks.dto.OrderDto;
import com.bigobooks.entities.orders.Order;
import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.entities.orders.OrderStatus;

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

	/**
	 * DTO del common -> entidad. Los ids de usuario y cupon (userId/couponId)
	 * no se resuelven aqui: el servicio los busca con sus propios
	 * repositorios; el mapper solo convierte datos planos.
	 */
	public Order toEntity(OrderDto dto) {
		if (dto == null) {
			return null;
		}
		Order order = new Order();
		order.setId(dto.getId());
		order.setWarehouseId(dto.getWarehouseId());
		order.setStatus(dto.getStatus() != null ? OrderStatus.valueOf(dto.getStatus().name()) : null);
		order.setTotalPrice(dto.getTotalPrice() != null ? dto.getTotalPrice() : 0L);
		order.setSubtotal(dto.getSubtotal() != null ? dto.getSubtotal() : 0L);
		order.setDiscountAmount(dto.getDiscountAmount() != null ? dto.getDiscountAmount() : 0L);
		order.setBancardNumber(dto.getBancardNumber());
		order.setInvoiceNumber(dto.getInvoiceNumber());
		order.setCreatedAt(dto.getCreatedAt());

		List<OrderDetail> details = new ArrayList<>();
		if (dto.getOrderDetails() != null) {
			for (OrderDetailDto detailDto : dto.getOrderDetails()) {
				if (detailDto == null) {
					continue;
				}
				OrderDetail detail = toEntity(detailDto);
				detail.setOrder(order);
				details.add(detail);
			}
		}
		order.setOrderDetails(details);
		return order;
	}

	public OrderDetail toEntity(OrderDetailDto dto) {
		if (dto == null) {
			return null;
		}
		OrderDetail detail = new OrderDetail();
		detail.setId(dto.getId());
		detail.setBookId(dto.getBookId());
		detail.setBookName(dto.getBookName());
		if (dto.getQuantity() != null) {
			detail.setQuantity(dto.getQuantity());
		}
		detail.setUnitPrice(dto.getUnitPrice() != null ? dto.getUnitPrice() : 0L);
		detail.setDiscount(dto.getDiscount() != null ? dto.getDiscount() : 0L);
		detail.setCreatedAt(dto.getCreatedAt());
		return detail;
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
