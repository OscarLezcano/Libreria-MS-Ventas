package com.bigobooks.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.bigobooks.model.Promotion;
import com.bigobooks.repository.BaseRepository;

public interface PromotionRepository extends BaseRepository<Promotion>, JpaSpecificationExecutor<Promotion> {

	/** Carga los libros de la promocion en la misma consulta (para el mapper). */
	@EntityGraph(attributePaths = "books")
	@Override
	Optional<Promotion> findById(Long id);

	@EntityGraph(attributePaths = "books")
	@Override
	Page<Promotion> findAll(Specification<Promotion> spec, Pageable pageable);

	@Override
	@Query(value = "SELECT * FROM sales_promotion", nativeQuery = true)
	List<Promotion> findAllIncludingDeleted();

	@Override
	@Query(value = "SELECT * FROM sales_promotion WHERE is_deleted = true", nativeQuery = true)
	List<Promotion> findDeleted();
}
