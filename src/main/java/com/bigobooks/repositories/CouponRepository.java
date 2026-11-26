package com.bigobooks.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.bigobooks.entities.orders.Coupon;
import com.bigobooks.repository.BaseRepository;

public interface CouponRepository extends BaseRepository<Coupon>, JpaSpecificationExecutor<Coupon> {

	@EntityGraph(attributePaths = "books")
	@Override
	Optional<Coupon> findById(Long id);

	@EntityGraph(attributePaths = "books")
	Optional<Coupon> findByCode(String code);

	@EntityGraph(attributePaths = "books")
	@Override
	Page<Coupon> findAll(Specification<Coupon> spec, Pageable pageable);

	boolean existsByCode(String code);

	@Override
	@Query(value = "SELECT * FROM coupon", nativeQuery = true)
	List<Coupon> findAllIncludingDeleted();

	@Override
	@Query(value = "SELECT * FROM coupon WHERE is_deleted = true", nativeQuery = true)
	List<Coupon> findDeleted();
}
