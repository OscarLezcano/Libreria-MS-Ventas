package com.bigobooks.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;

import com.bigobooks.entities.auth.Role;
import com.bigobooks.repository.BaseRepository;

public interface RoleRepository extends BaseRepository<Role> {

	Optional<Role> findByNameIgnoreCase(String name);

	@Override
	@Query(value = "SELECT * FROM role", nativeQuery = true)
	List<Role> findAllIncludingDeleted();

	@Override
	@Query(value = "SELECT * FROM role WHERE is_deleted = true", nativeQuery = true)
	List<Role> findDeleted();
}
