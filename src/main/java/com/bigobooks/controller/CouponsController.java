package com.bigobooks.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import com.bigobooks.dto.CouponCreateRequest;
import com.bigobooks.dto.CouponDto;
import com.bigobooks.dto.CouponUpdateRequest;
import com.bigobooks.dto.Envelope;
import com.bigobooks.entities.orders.Coupon;
import com.bigobooks.mappers.CouponMapper;
import com.bigobooks.services.CouponQueryService;
import com.bigobooks.services.CouponService;
import com.bigobooks.util.Envelopes;

/**
 * Controlador de cupones: implementa la interfaz generada desde openapi.yaml.
 */
@RestController
public class CouponsController implements CouponsApi {

	private static final Logger log = LoggerFactory.getLogger(CouponsController.class);

	private final CouponService couponService;
	private final CouponQueryService couponQueryService;
	private final CouponMapper couponMapper;

	public CouponsController(CouponService couponService, CouponQueryService couponQueryService,
			CouponMapper couponMapper) {
		this.couponService = couponService;
		this.couponQueryService = couponQueryService;
		this.couponMapper = couponMapper;
	}

	@Override
	@PreAuthorize("@roles.allows('coupons-manage')")
	public ResponseEntity<Envelope> createCoupon(CouponCreateRequest couponCreateRequest) {
		log.info("POST /coupons: creacion de cupon recibida");
		Coupon created = couponService.createFromRequest(couponCreateRequest);
		return new ResponseEntity<>(Envelopes.single(couponMapper.toDto(created)), HttpStatus.CREATED);
	}

	@Override
	@PreAuthorize("@roles.allows('coupons-view')")
	public ResponseEntity<Envelope> listCoupons(String code, Boolean active, Integer minDiscountPercent,
			Integer page, Integer size, String sort) {
		log.info("GET /coupons: listado de cupones (page={}, size={}, sort={})", page, size, sort);
		Page<Coupon> coupons = couponService.list(code, active, minDiscountPercent, page, size, sort);
		List<CouponDto> data = coupons.map(couponMapper::toDto).getContent();
		return ResponseEntity.ok(Envelopes.page(coupons, data));
	}

	@Override
	@PreAuthorize("@roles.allows('coupons-view')")
	public ResponseEntity<Envelope> getCouponById(Long id) {
		log.info("GET /coupons/{}: detalle de cupon", id);
		return ResponseEntity.ok(Envelopes.single(couponQueryService.getById(id)));
	}

	@Override
	@PreAuthorize("@roles.allows('coupons-manage')")
	public ResponseEntity<Envelope> updateCoupon(Long id, CouponUpdateRequest couponUpdateRequest) {
		log.info("PUT /coupons/{}: actualizacion de cupon recibida", id);
		return ResponseEntity.ok(Envelopes.single(couponMapper.toDto(couponService.update(id, couponUpdateRequest))));
	}

	@Override
	@PreAuthorize("@roles.allows('coupons-delete')")
	public ResponseEntity<Void> deleteCoupon(Long id) {
		log.info("DELETE /coupons/{}: borrado logico de cupon recibido", id);
		couponService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
