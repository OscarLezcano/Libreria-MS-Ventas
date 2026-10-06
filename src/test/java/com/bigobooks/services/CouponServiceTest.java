package com.bigobooks.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.bigobooks.dto.CouponCreateRequest;
import com.bigobooks.dto.CouponUpdateRequest;
import com.bigobooks.entities.orders.Coupon;
import com.bigobooks.exception.ConflictException;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.repositories.CouponRepository;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

	@Mock
	private CouponRepository repository;

	@InjectMocks
	private CouponService couponService;

	@Test
	void createGuardaElCupon() {
		when(repository.existsByCode("VERANO")).thenReturn(false);
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CouponCreateRequest request = new CouponCreateRequest();
		request.setCode(" VERANO ");
		request.setDiscountPercent(15);
		request.setValidUntil(LocalDateTime.of(2026, 12, 31, 23, 59));

		Coupon coupon = couponService.createFromRequest(request);

		assertEquals("VERANO", coupon.getCode());
		assertEquals(15, coupon.getDiscountPercent());
		assertEquals(LocalDateTime.of(2026, 12, 31, 23, 59), coupon.getValidUntil());
	}

	@Test
	void createRechazaCodigoRepetido() {
		when(repository.existsByCode("VERANO")).thenReturn(true);

		CouponCreateRequest request = new CouponCreateRequest();
		request.setCode("VERANO");
		request.setDiscountPercent(15);

		assertThrows(ConflictException.class, () -> couponService.createFromRequest(request));
	}

	@Test
	void createValidaElPorcentaje() {
		CouponCreateRequest request = new CouponCreateRequest();
		request.setCode("VERANO");
		request.setDiscountPercent(0);

		assertThrows(IllegalArgumentException.class, () -> couponService.createFromRequest(request));
	}

	@Test
	void updateValidaElPorcentaje() {
		when(repository.findById(1L)).thenReturn(Optional.of(new Coupon()));
		CouponUpdateRequest request = new CouponUpdateRequest();
		request.setDiscountPercent(150);

		assertThrows(IllegalArgumentException.class, () -> couponService.update(1L, request));
	}

	@Test
	void requireByCodeLanzaNotFound() {
		when(repository.findByCode("NOPE")).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> couponService.requireByCode("NOPE"));
	}

	@Test
	void listFiltraPorCodigo() {
		when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

		assertEquals(0, couponService.list("ver", null, null, 0, 20, null).getTotalElements());
	}
}
