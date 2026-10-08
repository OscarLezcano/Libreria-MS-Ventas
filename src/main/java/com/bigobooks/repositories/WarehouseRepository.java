package com.bigobooks.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import com.bigobooks.entities.stock.Warehouse;
import com.bigobooks.repository.BaseRepository;

public interface WarehouseRepository extends BaseRepository<Warehouse> {

	@Override
	@Query(value = "SELECT * FROM warehouse", nativeQuery = true)
	List<Warehouse> findAllIncludingDeleted();

	@Override
	@Query(value = "SELECT * FROM warehouse WHERE is_deleted = true", nativeQuery = true)
	List<Warehouse> findDeleted();
}
