package com.bigobooks.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import com.bigobooks.entities.orders.OrderDetail;
import com.bigobooks.repository.BaseRepository;

public interface OrderDetailRepository extends BaseRepository<OrderDetail> {

	@Override
	@Query(value = "SELECT * FROM order_detail", nativeQuery = true)
	List<OrderDetail> findAllIncludingDeleted();

	@Override
	@Query(value = "SELECT * FROM order_detail WHERE is_deleted = true", nativeQuery = true)
	List<OrderDetail> findDeleted();
}
