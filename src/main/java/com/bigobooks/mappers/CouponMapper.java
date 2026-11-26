package com.bigobooks.mappers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bigobooks.dto.CouponDto;
import com.bigobooks.entities.book.Book;
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
		dto.setValidFrom(coupon.getValidFrom());
		dto.setValidUntil(coupon.getValidUntil());
		dto.setSingleUsePerUser(coupon.isSingleUsePerUser());
		dto.setCreatedAt(coupon.getCreatedAt());

		List<Long> bookIds = new ArrayList<>();
		if (coupon.getBooks() != null) {
			for (Book book : coupon.getBooks()) {
				bookIds.add(book.getId());
			}
		}
		dto.setBookIds(bookIds);
		return dto;
	}

	/**
	 * DTO del common -> entidad. Las ordenes y los libros asociados al cupon
	 * no se resuelven aqui: los carga el servicio con sus repositorios.
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
		coupon.setValidFrom(dto.getValidFrom());
		coupon.setValidUntil(dto.getValidUntil());
		coupon.setSingleUsePerUser(Boolean.TRUE.equals(dto.getSingleUsePerUser()));
		coupon.setCreatedAt(dto.getCreatedAt());
		return coupon;
	}
}
