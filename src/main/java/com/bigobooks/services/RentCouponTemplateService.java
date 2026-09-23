package com.bigobooks.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigobooks.dto.RentCouponTemplateDto;
import com.bigobooks.entities.rents.RentCouponTemplate;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.mappers.RentCouponTemplateMapper;
import com.bigobooks.repositories.RentCouponTemplateRepository;
import com.bigobooks.specifications.RentCouponTemplateSpecifications;

/**
 * CRUD y listado de plantillas de cupones de renta (entidad simple del common).
 */
@Service
public class RentCouponTemplateService extends BaseService<RentCouponTemplate, Long, RentCouponTemplateRepository> {

	private final RentCouponTemplateMapper mapper;

	public RentCouponTemplateService(RentCouponTemplateRepository repository, RentCouponTemplateMapper mapper) {
		super(repository);
		this.mapper = mapper;
	}

	@Transactional
	@Override
	public RentCouponTemplate create(RentCouponTemplate entity) {
		validateTemplate(entity);
		return repository.save(entity);
	}

	@Transactional
	public RentCouponTemplate updateFromDto(Long id, RentCouponTemplateDto dto) {
		RentCouponTemplate existing = requireById(id);
		mapper.updateEntity(existing, dto);
		validateTemplate(existing);
		return repository.save(existing);
	}

	@Transactional
	@Override
	public void delete(Long id) {
		RentCouponTemplate template = requireById(id);
		template.setDeleted(true);
		repository.save(template);
	}

	public Page<RentCouponTemplate> list(String code, Boolean active, Integer minDiscountPercentage,
			int page, int size, String sort) {
		return repository.findAll(
				RentCouponTemplateSpecifications.build(code, active, minDiscountPercentage),
				buildPageable(page, size, sort));
	}

	public RentCouponTemplate requireById(Long id) {
		try {
			return getById(id);
		} catch (IllegalArgumentException e) {
			throw new NotFoundException("No existe la plantilla de cupon con id " + id);
		}
	}

	private void validateTemplate(RentCouponTemplate template) {
		if (template.getCode() == null || template.getCode().isBlank()) {
			throw new IllegalArgumentException("code es obligatorio");
		}
		int pct = template.getDiscountPercentage();
		if (pct < 0 || pct > 100) {
			throw new IllegalArgumentException("discountPercentage debe estar entre 0 y 100");
		}
		if (template.getStartDate() != null && template.getEndDate() != null
				&& template.getStartDate().isAfter(template.getEndDate())) {
			throw new IllegalArgumentException("startDate no puede ser posterior a endDate");
		}
	}

	private Pageable buildPageable(int page, int size, String sort) {
		Sort sortSpec = Sort.unsorted();
		if (sort != null && !sort.isBlank()) {
			String[] parts = sort.split(",");
			String field = parts[0].trim();
			Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
					? Sort.Direction.DESC
					: Sort.Direction.ASC;
			sortSpec = Sort.by(direction, field);
		}
		return PageRequest.of(Math.max(page, 0), Math.max(size, 1), sortSpec);
	}
}