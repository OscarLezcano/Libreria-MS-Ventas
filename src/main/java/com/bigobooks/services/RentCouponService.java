package com.bigobooks.services;

import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigobooks.entities.rents.Rent;
import com.bigobooks.entities.rents.RentCoupon;
import com.bigobooks.entities.rents.RentCouponTemplate;
import com.bigobooks.entities.rents.RentDetail;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.repositories.RentCouponRepository;
import com.bigobooks.repositories.RentCouponTemplateRepository;

/**
 * Gestion de la asociacion Rent <-> cupon aplicado (RentCoupon). Valida la
 * vigencia del cupon, su limite de usos, calcula el descuento y actualiza el
 * total de la renta.
 */
@Service
public class RentCouponService extends BaseService<RentCoupon, Long, RentCouponRepository> {

	private final RentCouponTemplateRepository templateRepository;

	public RentCouponService(RentCouponRepository repository, RentCouponTemplateRepository templateRepository) {
		super(repository);
		this.templateRepository = templateRepository;
	}

	@Transactional
	public RentCoupon applyCoupon(Rent rent, String couponCode) {
		if (couponCode == null || couponCode.isBlank()) {
			throw new IllegalArgumentException("couponCode es obligatorio");
		}
		RentCouponTemplate template = templateRepository.findByCode(couponCode.trim())
				.orElseThrow(() -> new NotFoundException("No existe el cupon con codigo " + couponCode.trim()));

		validateTemplate(template, couponCode.trim());

		long total = rent.getRentDetails() == null ? 0L
				: rent.getRentDetails().stream().mapToLong(RentDetail::getPrice).sum();
		long discount = total * template.getDiscountPercentage() / 100;
		if (template.getMaxDiscountAmount() != null && discount > template.getMaxDiscountAmount()) {
			discount = template.getMaxDiscountAmount();
		}

		RentCoupon rentCoupon = new RentCoupon();
		rentCoupon.setRent(rent);
		rentCoupon.setCoupon(template);
		rentCoupon.setDiscountPercentage(template.getDiscountPercentage());
		rentCoupon.setDiscountAmount(discount);
		rentCoupon.setAppliedAt(LocalDateTime.now());
		repository.save(rentCoupon);

		if (rent.getRentCoupons() == null) {
			rent.setRentCoupons(new ArrayList<>());
		}
		rent.getRentCoupons().add(rentCoupon);
		recalculateTotal(rent);
		return rentCoupon;
	}

	@Transactional
	public void removeCoupon(Rent rent, Long rentCouponId) {
		RentCoupon rentCoupon = repository.findById(rentCouponId)
				.orElseThrow(() -> new NotFoundException("No existe la aplicacion de cupon con id " + rentCouponId));

		if (rent.getRentCoupons() != null
				&& rent.getRentCoupons().stream().noneMatch(rc -> rc.getId().equals(rentCouponId))) {
			throw new IllegalArgumentException("El cupon aplicado no pertenece a la renta " + rent.getId());
		}

		repository.delete(rentCoupon);
		if (rent.getRentCoupons() != null) {
			rent.getRentCoupons().removeIf(rc -> rc.getId().equals(rentCouponId));
		}
		recalculateTotal(rent);
	}

	public void recalculateTotal(Rent rent) {
		long details = rent.getRentDetails() == null ? 0L
				: rent.getRentDetails().stream().mapToLong(RentDetail::getPrice).sum();
		long discounts = rent.getRentCoupons() == null ? 0L
				: rent.getRentCoupons().stream().mapToLong(RentCoupon::getDiscountAmount).sum();
		rent.setTotalPrice((int) Math.max(0L, details - discounts));
	}

	private void validateTemplate(RentCouponTemplate template, String couponCode) {
		LocalDateTime now = LocalDateTime.now();
		if (template.getStartDate() != null && now.isBefore(template.getStartDate())) {
			throw new IllegalArgumentException("El cupon " + couponCode + " no esta vigente");
		}
		if (template.getEndDate() != null && now.isAfter(template.getEndDate())) {
			throw new IllegalArgumentException("El cupon " + couponCode + " esta vencido");
		}
		if (template.getMaxUses() != null && repository.countByCouponId(template.getId()) >= template.getMaxUses()) {
			throw new IllegalArgumentException("El cupon " + couponCode + " alcanzo su limite de usos");
		}
	}
}