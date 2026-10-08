package com.bigobooks.mappers;

import org.springframework.stereotype.Component;

import com.bigobooks.dto.CouponDto;
import com.bigobooks.entities.orders.Coupon;

/**
 * Conversion bidireccional entre Cupon y su DTO del common.
 */
@Component
public class CouponMapper {

	public CouponDto toDto(Coupon coupon) {
		if (coupon == null) {
			return null;
		}
		CouponDto dto = new CouponDto();
		dto.setId(coupon.getId());
		dto.setCode(coupon.getCode());
		dto.setDiscountPercent(coupon.getDiscountPercent());
		dto.setValidUntil(coupon.getValidUntil());
		dto.setCreatedAt(coupon.getCreatedAt());
		return dto;
	}

	/**
	 * DTO del common -> entidad. Las ordenes asociadas al cupon no se
	 * resuelven aqui: las carga el servicio con su repositorio.
	 */
	public Coupon toEntity(CouponDto dto) {
		if (dto == null) {
			return null;
		}
		Coupon coupon = new Coupon();
		coupon.setId(dto.getId());
		coupon.setCode(dto.getCode());
		if (dto.getDiscountPercent() != null) {
			coupon.setDiscountPercent(dto.getDiscountPercent());
		}
		coupon.setValidUntil(dto.getValidUntil());
		coupon.setCreatedAt(dto.getCreatedAt());
		return coupon;
	}
}
