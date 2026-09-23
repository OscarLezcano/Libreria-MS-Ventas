package com.bigobooks.repositories;

import java.util.Optional;

import com.bigobooks.entities.rents.RentCouponTemplate;
import com.bigobooks.repositories.BaseRepository;

public interface RentCouponTemplateRepository extends BaseRepository<RentCouponTemplate, Long> {

	Optional<RentCouponTemplate> findByCode(String code);
}