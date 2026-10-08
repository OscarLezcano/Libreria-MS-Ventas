package com.bigobooks.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import com.bigobooks.entities.auth.UserAccount;
import com.bigobooks.repository.BaseRepository;

public interface UserAccountRepository extends BaseRepository<UserAccount> {

	@Override
	@Query(value = "SELECT * FROM user_account", nativeQuery = true)
	List<UserAccount> findAllIncludingDeleted();

	@Override
	@Query(value = "SELECT * FROM user_account WHERE is_deleted = true", nativeQuery = true)
	List<UserAccount> findDeleted();
}
