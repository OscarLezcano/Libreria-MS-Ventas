package com.bigobooks.services;

import com.bigobooks.entities.book.Book;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.repositories.BookRepository;

import org.springframework.stereotype.Service;

/**
 * Acceso a libros (entidad del common gestionada por otro microservicio).
 * Solo lectura: este servicio no crea ni modifica libros.
 */
@Service
public class BookService extends BaseService<Book, Long, BookRepository> {

	public BookService(BookRepository repository) {
		super(repository);
	}

	public Book requireById(Long id) {
		try {
			return getById(id);
		} catch (IllegalArgumentException e) {
			throw new NotFoundException("No existe el libro con id " + id);
		}
	}
}