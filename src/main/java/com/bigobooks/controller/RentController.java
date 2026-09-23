package com.bigobooks.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import com.bigobooks.dto.PageResponseDto;
import com.bigobooks.dto.RentCouponApply;
import com.bigobooks.dto.RentCreateRequest;
import com.bigobooks.dto.RentDto;
import com.bigobooks.dto.RentExtensionCreate;
import com.bigobooks.dto.RentExtensionDto;
import com.bigobooks.dto.RentUpdateRequest;
import com.bigobooks.entities.rents.Rent;
import com.bigobooks.entities.rents.RentExtension;
import com.bigobooks.mappers.RentExtensionMapper;
import com.bigobooks.mappers.RentMapper;
import com.bigobooks.services.RentCouponService;
import com.bigobooks.services.RentDetailService;
import com.bigobooks.services.RentService;

/**
 * Endpoints de /rents. Implementa la interfaz generada por OpenAPI
 * Generator; la transaccion a nivel de controlador mantiene abierta la sesion
 * mientras se mapean las colecciones perezosas de las entidades.
 */
@RestController
@Transactional
public class RentController implements RentsApi {

	private final RentService rentService;
	private final RentCouponService rentCouponService;
	private final RentDetailService rentDetailService;
	private final RentMapper rentMapper;
	private final RentExtensionMapper rentExtensionMapper;

	public RentController(RentService rentService, RentCouponService rentCouponService,
			RentDetailService rentDetailService, RentMapper rentMapper, RentExtensionMapper rentExtensionMapper) {
		this.rentService = rentService;
		this.rentCouponService = rentCouponService;
		this.rentDetailService = rentDetailService;
		this.rentMapper = rentMapper;
		this.rentExtensionMapper = rentExtensionMapper;
	}

	@Override
	public ResponseEntity<RentDto> createRent(RentCreateRequest rentCreateRequest) {
		Rent rent = rentService.createFromRequest(rentCreateRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(rentMapper.toDto(rent));
	}

	@Override
	public ResponseEntity<PageResponseDto> listRents(Long userId, Long bookId, LocalDateTime createdFrom,
			LocalDateTime createdTo, Boolean overdue, String couponCode, Integer page, Integer size, String sort) {
		Page<Rent> result = rentService.list(userId, bookId, createdFrom, createdTo, overdue, couponCode,
				page == null ? 0 : page, size == null ? 20 : size, sort);
		List<RentDto> content = result.getContent().stream().map(rentMapper::toDto).toList();
		return ResponseEntity.ok(PageResponseDto.from(result, content));
	}

	@Override
	public ResponseEntity<RentDto> getRentById(Long id) {
		return ResponseEntity.ok(rentMapper.toDto(rentService.requireById(id)));
	}

	@Override
	public ResponseEntity<RentDto> updateRent(Long id, RentUpdateRequest rentUpdateRequest) {
		Rent rent = rentService.updateUser(id, rentUpdateRequest);
		return ResponseEntity.ok(rentMapper.toDto(rent));
	}

	@Override
	public ResponseEntity<Void> deleteRent(Long id) {
		rentService.delete(id);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<RentDto> applyCouponToRent(Long id, RentCouponApply rentCouponApply) {
		Rent rent = rentService.requireById(id);
		rentCouponService.applyCoupon(rent, rentCouponApply.getCouponCode());
		return ResponseEntity.status(HttpStatus.CREATED).body(rentMapper.toDto(rent));
	}

	@Override
	public ResponseEntity<Void> removeCouponFromRent(Long id, Long rentCouponId) {
		Rent rent = rentService.requireById(id);
		rentCouponService.removeCoupon(rent, rentCouponId);
		return ResponseEntity.noContent().build();
	}

	@Override
	public ResponseEntity<RentExtensionDto> extendRent(Long rentDetailId, RentExtensionCreate rentExtensionCreate) {
		RentExtension extension = rentDetailService.extend(rentDetailId, rentExtensionCreate.getMonthsExtended());
		return ResponseEntity.status(HttpStatus.CREATED).body(rentExtensionMapper.toDto(extension));
	}
}