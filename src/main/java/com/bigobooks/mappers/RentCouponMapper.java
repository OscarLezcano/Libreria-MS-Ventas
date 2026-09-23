package com.bigobooks.mappers;

import org.springframework.stereotype.Component;

import com.bigobooks.dto.RentCouponDto;
import com.bigobooks.entities.rents.RentCoupon;

@Component
public class RentCouponMapper {

	public RentCouponDto toDto(RentCoupon rentCoupon) {
		RentCouponDto dto = new RentCouponDto();
		dto.setId(rentCoupon.getId());
		dto.setRentId(rentCoupon.getRent() != null ? rentCoupon.getRent().getId() : null);
		dto.setCouponTemplateId(rentCoupon.getCoupon() != null ? rentCoupon.getCoupon().getId() : null);
		dto.setDiscountPercentage(rentCoupon.getDiscountPercentage());
		dto.setDiscountAmount(rentCoupon.getDiscountAmount());
		dto.setAppliedAt(rentCoupon.getAppliedAt());
		return dto;
	}
}