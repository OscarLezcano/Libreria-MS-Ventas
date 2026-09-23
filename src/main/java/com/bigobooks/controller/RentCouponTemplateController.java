package com.bigobooks.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.bigobooks.dto.PageResponseDto;
import com.bigobooks.dto.RentCouponTemplateDto;
import com.bigobooks.entities.rents.RentCouponTemplate;
import com.bigobooks.mappers.RentCouponTemplateMapper;
import com.bigobooks.services.RentCouponTemplateService;

/**
 * Endpoints de /rent-coupon-templates. Implementa la interfaz generada por
 * OpenAPI Generator (CRUD directo de una entidad simple).
 */
@RestController
public class RentCouponTemplateController implements RentCouponTemplatesApi {

	private final RentCouponTemplateService templateService;
	private final RentCouponTemplateMapper templateMapper;

	public RentCouponTemplateController(RentCouponTemplateService templateService,
			RentCouponTemplateMapper templateMapper) {
		this.templateService = templateService;
		this.templateMapper = templateMapper;
	}

	@Override
	public ResponseEntity<RentCouponTemplateDto> createRentCouponTemplate(
			RentCouponTemplateDto rentCouponTemplateDto) {
		RentCouponTemplate template = templateService.create(templateMapper.toEntity(rentCouponTemplateDto));
		return ResponseEntity.status(HttpStatus.CREATED).body(templateMapper.toDto(template));
	}

	@Override
	public ResponseEntity<PageResponseDto> listRentCouponTemplates(String code, Boolean active,
			Integer minDiscountPercentage, Integer page, Integer size, String sort) {
		Page<RentCouponTemplate> result = templateService.list(code, active, minDiscountPercentage,
				page == null ? 0 : page, size == null ? 20 : size, sort);
		List<RentCouponTemplateDto> content = result.getContent().stream().map(templateMapper::toDto).toList();
		return ResponseEntity.ok(PageResponseDto.from(result, content));
	}

	@Override
	public ResponseEntity<RentCouponTemplateDto> getRentCouponTemplateById(Long id) {
		return ResponseEntity.ok(templateMapper.toDto(templateService.requireById(id)));
	}

	@Override
	public ResponseEntity<RentCouponTemplateDto> updateRentCouponTemplate(Long id,
			RentCouponTemplateDto rentCouponTemplateDto) {
		RentCouponTemplate template = templateService.updateFromDto(id, rentCouponTemplateDto);
		return ResponseEntity.ok(templateMapper.toDto(template));
	}

	@Override
	public ResponseEntity<Void> deleteRentCouponTemplate(Long id) {
		templateService.delete(id);
		return ResponseEntity.noContent().build();
	}
}