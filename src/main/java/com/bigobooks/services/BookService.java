package com.bigobooks.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bigobooks.entities.book.Book;
import com.bigobooks.util.exception.NotFoundException;
import com.bigobooks.util.exception.ResourceType;
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

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public Book requireById(Long id) {
		return findById(id).orElseThrow(() -> NotFoundException.byId(ResourceType.BOOK, id));
	}
}
