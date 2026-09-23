package com.bigobooks.repositories;

import com.bigobooks.entities.rents.RentCoupon;
import com.bigobooks.repositories.BaseRepository;

public interface RentCouponRepository extends BaseRepository<RentCoupon, Long> {

	long countByCouponId(Long couponId);
}