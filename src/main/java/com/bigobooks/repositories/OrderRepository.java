package com.bigobooks.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigobooks.entities.orders.Order;
import com.bigobooks.repository.BaseRepository;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends BaseRepository<Order>, JpaSpecificationExecutor<Order> {

	/** Trae la venta con sus detalles ya inicializados (para el mapper). */
	@EntityGraph(attributePaths = { "orderDetails", "userAccount", "coupon" })
	@Query("select o from Order o where o.id = :id")
	Optional<Order> findByIdWithDetails(@Param("id") Long id);

	/** Pedido publico por numeroOrden con detalle, usuario y cupon inicializados. */
	@EntityGraph(attributePaths = { "orderDetails", "userAccount", "coupon" })
	@Query("select o from Order o where o.numeroOrden = :numeroOrden")
	Optional<Order> findByNumeroOrdenWithDetails(@Param("numeroOrden") String numeroOrden);

	/** Bloqueo pesimista para que el pago concurrente no descuente stock dos veces. */
	@EntityGraph(attributePaths = { "orderDetails", "userAccount", "coupon" })
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select o from Order o where o.numeroOrden = :numeroOrden")
	Optional<Order> findByNumeroOrdenForUpdate(@Param("numeroOrden") String numeroOrden);

	boolean existsByNumeroOrden(String numeroOrden);

	@Query("select o from Order o where o.idempotencyKey = :idempotencyKey and o.userAccount.id = :userId")
	Optional<Order> findByIdempotencyKeyAndUserId(@Param("idempotencyKey") String idempotencyKey,
			@Param("userId") Long userId);

	@EntityGraph(attributePaths = { "userAccount", "coupon" })
	@Override
	Page<Order> findAll(Specification<Order> spec, Pageable pageable);

	@Override
	@Query(value = "SELECT * FROM orders", nativeQuery = true)
	List<Order> findAllIncludingDeleted();

	@Override
	@Query(value = "SELECT * FROM orders WHERE is_deleted = true", nativeQuery = true)
	List<Order> findDeleted();
}
