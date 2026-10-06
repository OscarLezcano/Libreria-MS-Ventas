package com.bigobooks.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigobooks.dto.CouponCreateRequest;
import com.bigobooks.dto.CouponUpdateRequest;
import com.bigobooks.entities.orders.Coupon;
import com.bigobooks.exception.ConflictException;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.repositories.CouponRepository;
import com.bigobooks.service.BaseService;
import com.bigobooks.specifications.CouponSpecifications;

@Service
public class CouponService extends BaseService<Coupon, CouponRepository> {

	private static final Logger log = LoggerFactory.getLogger(CouponService.class);

	public CouponService(CouponRepository repository) {
		super(repository);
	}

	public Coupon requireById(Long id) {
		return findById(id).orElseThrow(() -> new NotFoundException("No existe el cupon con id " + id));
	}

	public Coupon requireByCode(String code) {
		return getRepository().findByCode(code.trim())
				.orElseThrow(() -> new NotFoundException("No existe el cupon con codigo " + code));
	}

	@Transactional
	public Coupon createFromRequest(CouponCreateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Debe indicar los datos del cupon");
		}
		String code = request.getCode() != null ? request.getCode().trim() : "";
		int discountPercent = requirePercent(request.getDiscountPercent());
		if (code.isEmpty()) {
			throw new IllegalArgumentException("El codigo del cupon es obligatorio");
		}
		if (getRepository().existsByCode(code)) {
			throw new ConflictException("Ya existe un cupon con el codigo " + code);
		}

		Coupon coupon = new Coupon();
		coupon.setCode(code);
		coupon.setDiscountPercent(discountPercent);
		coupon.setValidUntil(request.getValidUntil());
		Coupon saved = getRepository().save(coupon);
		log.info("Cupon {} creado ({}% de descuento, vence {})", saved.getCode(), saved.getDiscountPercent(),
				saved.getValidUntil());
		return saved;
	}

	@Transactional
	public Coupon update(Long id, CouponUpdateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Debe indicar los datos del cupon");
		}
		Coupon coupon = requireById(id);
		coupon.setDiscountPercent(requirePercent(request.getDiscountPercent()));
		coupon.setValidUntil(request.getValidUntil());
		Coupon saved = getRepository().save(coupon);
		log.info("Cupon {} actualizado ({}%)", saved.getCode(), saved.getDiscountPercent());
		return saved;
	}

	public Page<Coupon> list(String code, Boolean active, Integer minDiscountPercent, Integer page, Integer size,
			String sort) {
		Page<Coupon> result = getRepository().findAll(
				CouponSpecifications.build(code, active, minDiscountPercent), Pageables.of(page, size, sort));
		log.debug("Listado de cupones: pagina {} con {} resultado(s)", result.getNumber(), result.getTotalElements());
		return result;
	}

	@Transactional
	public void delete(Long id) {
		Coupon coupon = requireById(id);
		coupon.setDeleted(true);
		getRepository().save(coupon);
		log.info("Cupon {} eliminado (borrado logico)", coupon.getCode());
	}

	private int requirePercent(Integer value) {
		if (value == null) {
			throw new IllegalArgumentException("discountPercent es obligatorio");
		}
		if (value < 1 || value > 100) {
			throw new IllegalArgumentException("discountPercent debe estar entre 1 y 100");
		}
		return value;
	}
}
