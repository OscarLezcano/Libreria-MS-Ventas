package com.bigobooks.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.bigobooks.dto.CouponDto;
import com.bigobooks.entities.orders.Coupon;

/**
 * Conversion bidireccional Cupon &lt;-&gt; DTO (Entrega #2).
 */
class CouponMapperTest {

	private final CouponMapper mapper = new CouponMapper();

	@Test
	void convierteEntidadADto() {
		Coupon coupon = new Coupon();
		coupon.setId(7L);
		coupon.setCode("VERANO");
		coupon.setDiscountPercent(15);
		coupon.setValidUntil(LocalDateTime.of(2026, 12, 31, 23, 59));

		CouponDto dto = mapper.toDto(coupon);

		assertEquals(7L, dto.getId());
		assertEquals("VERANO", dto.getCode());
		assertEquals(15, dto.getDiscountPercent());
		assertEquals(LocalDateTime.of(2026, 12, 31, 23, 59), dto.getValidUntil());
	}

	@Test
	void convierteDtoAEntidad() {
		CouponDto dto = new CouponDto();
		dto.setId(7L);
		dto.setCode("VERANO");
		dto.setDiscountPercent(15);
		dto.setValidUntil(LocalDateTime.of(2026, 12, 31, 23, 59));

		Coupon coupon = mapper.toEntity(dto);

		assertEquals(7L, coupon.getId());
		assertEquals("VERANO", coupon.getCode());
		assertEquals(15, coupon.getDiscountPercent());
		assertEquals(LocalDateTime.of(2026, 12, 31, 23, 59), coupon.getValidUntil());
	}

	@Test
	void elRoundtripConservaLosCampos() {
		Coupon coupon = new Coupon();
		coupon.setId(7L);
		coupon.setCode("VERANO");
		coupon.setDiscountPercent(15);

		CouponDto dto = mapper.toDto(mapper.toEntity(mapper.toDto(coupon)));

		assertEquals(coupon.getId(), dto.getId());
		assertEquals(coupon.getCode(), dto.getCode());
		assertEquals(coupon.getDiscountPercent(), dto.getDiscountPercent());
	}

	@Test
	void convierteValoresNulos() {
		assertNull(mapper.toDto(null));
		assertNull(mapper.toEntity(null));
	}
}
