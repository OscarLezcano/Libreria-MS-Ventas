package com.bigobooks.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.bigobooks.entities.orders.Coupon;
import com.bigobooks.repository.BaseRepository;

public interface CouponRepository extends BaseRepository<Coupon>, JpaSpecificationExecutor<Coupon> {

	Optional<Coupon> findByCode(String code);

	boolean existsByCode(String code);

	@Override
	@Query(value = "SELECT * FROM coupon", nativeQuery = true)
	List<Coupon> findAllIncludingDeleted();

	@Override
	@Query(value = "SELECT * FROM coupon WHERE is_deleted = true", nativeQuery = true)
	List<Coupon> findDeleted();
}
