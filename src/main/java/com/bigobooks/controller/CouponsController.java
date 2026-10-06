package com.bigobooks.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.bigobooks.dto.CouponCreateRequest;
import com.bigobooks.dto.CouponDto;
import com.bigobooks.dto.CouponUpdateRequest;
import com.bigobooks.dto.PageResponseDto;
import com.bigobooks.entities.orders.Coupon;
import com.bigobooks.mappers.CouponMapper;
import com.bigobooks.services.CouponService;

/**
 * Controlador de cupones: implementa la interfaz generada desde openapi.yaml.
 */
@RestController
public class CouponsController implements CouponsApi {

	private final CouponService couponService;
	private final CouponMapper couponMapper;

	public CouponsController(CouponService couponService, CouponMapper couponMapper) {
		this.couponService = couponService;
		this.couponMapper = couponMapper;
	}

	@Override
	public ResponseEntity<CouponDto> createCoupon(CouponCreateRequest couponCreateRequest) {
		Coupon created = couponService.createFromRequest(couponCreateRequest);
		return new ResponseEntity<>(couponMapper.toDto(created), HttpStatus.CREATED);
	}

	@Override
	public ResponseEntity<PageResponseDto> listCoupons(String code, Boolean active, Integer minDiscountPercent,
			Integer page, Integer size, String sort) {
		Page<Coupon> coupons = couponService.list(code, active, minDiscountPercent, page, size, sort);
		return ResponseEntity.ok(PageResponseDto.from(coupons.map(couponMapper::toDto)));
	}

	@Override
	public ResponseEntity<CouponDto> getCouponById(Long id) {
		return ResponseEntity.ok(couponMapper.toDto(couponService.requireById(id)));
	}

	@Override
	public ResponseEntity<CouponDto> updateCoupon(Long id, CouponUpdateRequest couponUpdateRequest) {
		return ResponseEntity.ok(couponMapper.toDto(couponService.update(id, couponUpdateRequest)));
	}

	@Override
	public ResponseEntity<Void> deleteCoupon(Long id) {
		couponService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
