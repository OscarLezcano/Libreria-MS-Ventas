package com.bigobooks.services;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigobooks.dto.PromotionBookAdd;
import com.bigobooks.dto.PromotionCreateRequest;
import com.bigobooks.dto.PromotionUpdateRequest;
import com.bigobooks.entities.book.Book;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.model.Promotion;
import com.bigobooks.repositories.PromotionRepository;
import com.bigobooks.service.BaseService;
import com.bigobooks.specifications.PromotionSpecifications;

@Service
public class PromotionService extends BaseService<Promotion, PromotionRepository> {

	private static final Logger log = LoggerFactory.getLogger(PromotionService.class);

	private final BookService bookService;

	public PromotionService(PromotionRepository repository, BookService bookService) {
		super(repository);
		this.bookService = bookService;
	}

	public Promotion requireById(Long id) {
		return findById(id).orElseThrow(() -> new NotFoundException("No existe la promocion con id " + id));
	}

	@Transactional
	public Promotion createFromRequest(PromotionCreateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Debe indicar los datos de la promocion");
		}
		String name = request.getName() != null ? request.getName().trim() : "";
		int discountPercent = requirePercent(request.getDiscountPercent());
		if (name.isEmpty()) {
			throw new IllegalArgumentException("El nombre de la promocion es obligatorio");
		}
		validateDates(request.getStartDate(), request.getEndDate());

		Promotion promotion = new Promotion();
		promotion.setName(name);
		promotion.setDiscountPercent(discountPercent);
		promotion.setStartDate(request.getStartDate());
		promotion.setEndDate(request.getEndDate());
		promotion.setBooks(resolveBooks(request.getBookIds()));
		Promotion saved = getRepository().save(promotion);
		log.info("Promocion {} creada ({}%, {} libro(s))", saved.getName(), saved.getDiscountPercent(),
				saved.getBooks().size());
		return saved;
	}

	@Transactional
	public Promotion update(Long id, PromotionUpdateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Debe indicar los datos de la promocion");
		}
		Promotion promotion = requireById(id);
		String name = request.getName() != null ? request.getName().trim() : "";
		if (name.isEmpty()) {
			throw new IllegalArgumentException("El nombre de la promocion es obligatorio");
		}
		validateDates(request.getStartDate(), request.getEndDate());

		promotion.setName(name);
		promotion.setDiscountPercent(requirePercent(request.getDiscountPercent()));
		promotion.setStartDate(request.getStartDate());
		promotion.setEndDate(request.getEndDate());
		Promotion saved = getRepository().save(promotion);
		log.info("Promocion {} actualizada ({}%)", saved.getName(), saved.getDiscountPercent());
		return saved;
	}

	@Transactional
	public Promotion addBooks(Long id, PromotionBookAdd request) {
		if (request == null || request.getBookIds() == null || request.getBookIds().isEmpty()) {
			throw new IllegalArgumentException("Debe indicar al menos un libro");
		}
		Promotion promotion = requireById(id);
		List<Book> books = promotion.getBooks() != null ? new ArrayList<>(promotion.getBooks()) : new ArrayList<>();
		for (Long bookId : request.getBookIds()) {
			if (bookId == null) {
				throw new IllegalArgumentException("bookIds no puede contener null");
			}
			Book book = bookService.requireById(bookId);
			boolean alreadyLinked = books.stream().anyMatch(existing -> existing.getId().equals(book.getId()));
			if (!alreadyLinked) {
				books.add(book);
			}
		}
		promotion.setBooks(books);
		Promotion saved = getRepository().save(promotion);
		log.info("Promocion {}: {} libro(s) asociados", saved.getId(), saved.getBooks().size());
		return saved;
	}

	@Transactional
	public Promotion removeBook(Long id, Long bookId) {
		Promotion promotion = requireById(id);
		List<Book> books = promotion.getBooks() != null ? new ArrayList<>(promotion.getBooks()) : new ArrayList<>();
		boolean removed = books.removeIf(existing -> existing.getId().equals(bookId));
		if (!removed) {
			throw new NotFoundException("El libro " + bookId + " no esta asociado a la promocion " + id);
		}
		promotion.setBooks(books);
		Promotion saved = getRepository().save(promotion);
		log.info("Promocion {}: libro {} desasociado", saved.getId(), bookId);
		return saved;
	}

	public Page<Promotion> list(String name, Boolean active, Integer minDiscountPercent, Long bookId, Integer page,
			Integer size, String sort) {
		Page<Promotion> result = getRepository().findAll(
				PromotionSpecifications.build(name, active, minDiscountPercent, bookId), Pageables.of(page, size, sort));
		log.debug("Listado de promociones: pagina {} con {} resultado(s)", result.getNumber(),
				result.getTotalElements());
		return result;
	}

	@Transactional
	public void delete(Long id) {
		Promotion promotion = requireById(id);
		promotion.setDeleted(true);
		getRepository().save(promotion);
		log.info("Promocion {} eliminada (borrado logico)", promotion.getName());
	}

	private List<Book> resolveBooks(List<Long> bookIds) {
		List<Book> books = new ArrayList<>();
		if (bookIds == null) {
			return books;
		}
		for (Long bookId : bookIds) {
			if (bookId == null) {
				throw new IllegalArgumentException("bookIds no puede contener null");
			}
			books.add(bookService.requireById(bookId));
		}
		return books;
	}

	private void validateDates(LocalDate startDate, LocalDate endDate) {
		if (startDate == null || endDate == null) {
			throw new IllegalArgumentException("startDate y endDate son obligatorios");
		}
		if (endDate.isBefore(startDate)) {
			throw new IllegalArgumentException("endDate no puede ser anterior a startDate");
		}
	}

	private int requirePercent(Integer value) {
		if (value == null) {
			throw new IllegalArgumentException("discountPercent es obligatorio");
		}
		if (value < 1 || value > 100) {
			throw new IllegalArgumentException("discountPercent debe estar entre 1 y 100");
		}
		return value;
	}
}
