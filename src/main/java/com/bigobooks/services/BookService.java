package com.bigobooks.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger log = LoggerFactory.getLogger(BookService.class);

	public BookService(BookRepository repository) {
		super(repository);
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public Book requireById(Long id) {
		return findById(id).orElseThrow(() -> NotFoundException.byId(ResourceType.BOOK, id));
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public String transactionRequired() {
		return logTransaction("REQUIRED");
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public String transactionRequiresNew() {
		return logTransaction("REQUIRES_NEW");
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public String transactionSupports() {
		return logTransaction("SUPPORTS");
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public String transactionNotSupported() {
		return logTransaction("NOT_SUPPORTED");
	}

	@Transactional(propagation = Propagation.NEVER)
	public String transactionNever() {
		return logTransaction("NEVER");
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public String transactionMandatory() {
		return logTransaction("MANDATORY");
	}

	private String logTransaction(String propagation) {
		boolean active = org.springframework.transaction.support.TransactionSynchronizationManager
				.isActualTransactionActive();
		String result = propagation + " active=" + active;
		log.info("TX-DEMO {}", result);
		return result;
	}
}
