package com.bigobooks.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.Query;

import com.bigobooks.entities.book.Book;
import com.bigobooks.repository.BaseRepository;

public interface BookRepository extends BaseRepository<Book> {

	@Override
	@Query(value = "SELECT * FROM book", nativeQuery = true)
	List<Book> findAllIncludingDeleted();

	@Override
	@Query(value = "SELECT * FROM book WHERE is_deleted = true", nativeQuery = true)
	List<Book> findDeleted();
}
