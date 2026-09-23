package com.bigobooks.mappers;

import org.springframework.stereotype.Component;

import com.bigobooks.dto.RentCouponTemplateDto;
import com.bigobooks.entities.rents.RentCouponTemplate;

@Component
public class RentCouponTemplateMapper {

	public RentCouponTemplateDto toDto(RentCouponTemplate template) {
		RentCouponTemplateDto dto = new RentCouponTemplateDto();
		dto.setId(template.getId());
		dto.setCode(template.getCode());
		dto.setDiscountPercentage(template.getDiscountPercentage());
		dto.setStartDate(template.getStartDate());
		dto.setEndDate(template.getEndDate());
		dto.setMaxUses(template.getMaxUses());
		dto.setMaxDiscountAmount(template.getMaxDiscountAmount());
		dto.setCreatedAt(template.getCreatedAt());
		return dto;
	}

	public RentCouponTemplate toEntity(RentCouponTemplateDto dto) {
		RentCouponTemplate entity = new RentCouponTemplate();
		entity.setCode(dto.getCode());
		entity.setDiscountPercentage(dto.getDiscountPercentage() != null ? dto.getDiscountPercentage() : 0);
		entity.setStartDate(dto.getStartDate());
		entity.setEndDate(dto.getEndDate());
		entity.setMaxUses(dto.getMaxUses());
		entity.setMaxDiscountAmount(dto.getMaxDiscountAmount());
		return entity;
	}

	public void updateEntity(RentCouponTemplate entity, RentCouponTemplateDto dto) {
		if (dto.getCode() != null) {
			entity.setCode(dto.getCode());
		}
		if (dto.getDiscountPercentage() != null) {
			entity.setDiscountPercentage(dto.getDiscountPercentage());
		}
		if (dto.getStartDate() != null) {
			entity.setStartDate(dto.getStartDate());
		}
		if (dto.getEndDate() != null) {
			entity.setEndDate(dto.getEndDate());
		}
		if (dto.getMaxUses() != null) {
			entity.setMaxUses(dto.getMaxUses());
		}
		if (dto.getMaxDiscountAmount() != null) {
			entity.setMaxDiscountAmount(dto.getMaxDiscountAmount());
		}
	}
}