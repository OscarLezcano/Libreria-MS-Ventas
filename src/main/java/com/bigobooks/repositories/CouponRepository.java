package com.bigobooks.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.bigobooks.entities.orders.Coupon;
import com.bigobooks.repository.BaseRepository;

public interface CouponRepository extends BaseRepository<Coupon>, JpaSpecificationExecutor<Coupon> {

	Optional<Coupon> findByCode(String code);

	boolean existsByCode(String code);
}
