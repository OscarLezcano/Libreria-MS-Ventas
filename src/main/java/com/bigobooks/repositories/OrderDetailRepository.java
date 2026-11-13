package com.bigobooks.repositories;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.entities.orders.OrderStatus;
import com.bigobooks.repository.BaseRepository;

public interface OrderDetailRepository extends BaseRepository<OrderDetail> {

	@Override
	@Query(value = "SELECT * FROM order_detail", nativeQuery = true)
	List<OrderDetail> findAllIncludingDeleted();

	@Override
	@Query(value = "SELECT * FROM order_detail WHERE is_deleted = true", nativeQuery = true)
	List<OrderDetail> findDeleted();

	/**
	 * Agrega las unidades vendidas por libro en ventas pagadas (PAID). El
	 * Pageable se usa solo para limitar el resultado (top N).
	 */
	@Query("""
			select d.bookId as bookId, d.bookName as bookName, sum(d.quantity) as totalSold
			from OrderDetail d
			join d.order o
			where o.status = :status and o.isDeleted = false and d.isDeleted = false
			group by d.bookId, d.bookName
			order by sum(d.quantity) desc, d.bookName asc
			""")
	List<BestSellingBookProjection> findBestSellingBooks(@Param("status") OrderStatus status, Pageable pageable);

	/** Proyeccion del reporte de libros mas vendidos. */
	interface BestSellingBookProjection {

		Long getBookId();

		String getBookName();

		Long getTotalSold();
	}
}
