package com.bigobooks.services;

import org.springframework.stereotype.Service;

import com.bigobooks.entities.book.Book;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.repositories.BookRepository;
import com.bigobooks.service.BaseService;

/**
 * Servicio de apoyo (sin controlador): acceso de solo lectura a los libros
 * del catalogo compartido.
 */
@Service
public class BookService extends BaseService<Book, BookRepository> {

	public BookService(BookRepository repository) {
		super(repository);
	}

	public Book requireById(Long id) {
		return findById(id).orElseThrow(() -> new NotFoundException("No existe el libro con id " + id));
	}
}
