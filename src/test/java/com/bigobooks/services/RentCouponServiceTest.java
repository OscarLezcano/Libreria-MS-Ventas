package com.bigobooks.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bigobooks.entities.rents.Rent;
import com.bigobooks.entities.rents.RentCoupon;
import com.bigobooks.entities.rents.RentCouponTemplate;
import com.bigobooks.entities.rents.RentDetail;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.repositories.RentCouponRepository;
import com.bigobooks.repositories.RentCouponTemplateRepository;

@ExtendWith(MockitoExtension.class)
class RentCouponServiceTest {

	@Mock
	private RentCouponRepository repository;
	@Mock
	private RentCouponTemplateRepository templateRepository;

	@InjectMocks
	private RentCouponService rentCouponService;

	private Rent rentWithDetail(long price) {
		Rent rent = new Rent();
		rent.setRentDetails(new ArrayList<>());
		rent.setRentCoupons(new ArrayList<>());
		RentDetail detail = new RentDetail();
		detail.setPrice(price);
		rent.getRentDetails().add(detail);
		return rent;
	}

	private RentCouponTemplate template(int pct) {
		RentCouponTemplate template = new RentCouponTemplate();
		template.setId(1L);
		template.setCode("VERANO10");
		template.setDiscountPercentage(pct);
		template.setStartDate(LocalDateTime.now().minusDays(1));
		template.setEndDate(LocalDateTime.now().plusDays(1));
		return template;
	}

	@Test
	void applyCouponCalculaDescuentoYActualizaTotal() {
		Rent rent = rentWithDetail(2000L);
		RentCouponTemplate template = template(10);
		when(templateRepository.findByCode("VERANO10")).thenReturn(Optional.of(template));
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		RentCoupon coupon = rentCouponService.applyCoupon(rent, "VERANO10");

		assertEquals(200L, coupon.getDiscountAmount());
		assertEquals(10, coupon.getDiscountPercentage());
		assertEquals(1, rent.getRentCoupons().size());
		assertEquals(1800, rent.getTotalPrice());
	}

	@Test
	void applyCouponRespetaMaxDiscountAmount() {
		Rent rent = rentWithDetail(2000L);
		RentCouponTemplate template = template(30);
		template.setMaxDiscountAmount(500L);
		when(templateRepository.findByCode("VERANO10")).thenReturn(Optional.of(template));
		when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		rentCouponService.applyCoupon(rent, "VERANO10");

		assertEquals(500L, rent.getRentCoupons().get(0).getDiscountAmount());
		assertEquals(1500, rent.getTotalPrice());
	}

	@Test
	void applyCouponRechazaCuponVencido() {
		Rent rent = rentWithDetail(2000L);
		RentCouponTemplate template = template(10);
		template.setEndDate(LocalDateTime.now().minusMinutes(1));
		when(templateRepository.findByCode("VERANO10")).thenReturn(Optional.of(template));

		assertThrows(IllegalArgumentException.class, () -> rentCouponService.applyCoupon(rent, "VERANO10"));
	}

	@Test
	void applyCouponRechazaLimiteDeUsos() {
		Rent rent = rentWithDetail(2000L);
		RentCouponTemplate template = template(10);
		template.setMaxUses(2);
		when(templateRepository.findByCode("VERANO10")).thenReturn(Optional.of(template));
		when(repository.countByCouponId(1L)).thenReturn(2L);

		assertThrows(IllegalArgumentException.class, () -> rentCouponService.applyCoupon(rent, "VERANO10"));
	}

	@Test
	void applyCouponNoEncuentraCupon() {
		Rent rent = rentWithDetail(2000L);
		when(templateRepository.findByCode("NOEXISTE")).thenReturn(Optional.empty());

		assertThrows(NotFoundException.class, () -> rentCouponService.applyCoupon(rent, "NOEXISTE"));
	}
}