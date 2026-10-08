package com.bigobooks.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigobooks.entities.orders.Order;
import com.bigobooks.repository.BaseRepository;

public interface OrderRepository extends BaseRepository<Order>, JpaSpecificationExecutor<Order> {

	/** Trae la venta con sus detalles ya inicializados (para el mapper). */
	@EntityGraph(attributePaths = "orderDetails")
	@Query("select o from Order o where o.id = :id")
	Optional<Order> findByIdWithDetails(@Param("id") Long id);

	@EntityGraph(attributePaths = "orderDetails")
	@Override
	Page<Order> findAll(Specification<Order> spec, Pageable pageable);

	@Override
	@Query(value = "SELECT * FROM orders", nativeQuery = true)
	List<Order> findAllIncludingDeleted();

	@Override
	@Query(value = "SELECT * FROM orders WHERE is_deleted = true", nativeQuery = true)
	List<Order> findDeleted();
}
