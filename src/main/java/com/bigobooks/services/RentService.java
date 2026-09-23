package com.bigobooks.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigobooks.dto.RentCreateRequest;
import com.bigobooks.dto.RentItemCreate;
import com.bigobooks.dto.RentUpdateRequest;
import com.bigobooks.entities.book.Book;
import com.bigobooks.entities.rents.Rent;
import com.bigobooks.entities.rents.RentDetail;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.repositories.RentDetailRepository;
import com.bigobooks.repositories.RentRepository;
import com.bigobooks.specifications.RentSpecifications;

/**
 * Servicio principal de rentas: creacion con detalles, actualizacion del
 * usuario, listado con filtros, y elimnacion logica sobrescrita.
 */
@Service
public class RentService extends BaseService<Rent, Long, RentRepository> {

	private final BookService bookService;
	private final UserAccountService userAccountService;
	private final RentCouponService rentCouponService;
	private final RentDetailRepository rentDetailRepository;

	public RentService(RentRepository repository, BookService bookService, UserAccountService userAccountService,
			RentCouponService rentCouponService, RentDetailRepository rentDetailRepository) {
		super(repository);
		this.bookService = bookService;
		this.userAccountService = userAccountService;
		this.rentCouponService = rentCouponService;
		this.rentDetailRepository = rentDetailRepository;
	}

	public Rent requireById(Long id) {
		try {
			return getById(id);
		} catch (IllegalArgumentException e) {
			throw new NotFoundException("No existe la renta con id " + id);
		}
	}

	@Transactional
	public Rent createFromRequest(RentCreateRequest request) {
		if (request.getItems() == null || request.getItems().isEmpty()) {
			throw new IllegalArgumentException("Debe incluir al menos un item en la renta");
		}

		Rent rent = new Rent();
		rent.setRentDetails(new ArrayList<>());
		rent.setRentCoupons(new ArrayList<>());
		if (request.getUserId() != null) {
			rent.setUserAccount(userAccountService.requireById(request.getUserId()));
		}

		Rent saved = repository.save(rent);

		for (RentItemCreate item : request.getItems()) {
			if (item.getBookId() == null || item.getMonthsRented() == null) {
				throw new IllegalArgumentException("Cada item requiere bookId y monthsRented");
			}
			if (item.getMonthsRented() <= 0) {
				throw new IllegalArgumentException("monthsRented debe ser mayor a 0");
			}
			Book book = bookService.requireById(item.getBookId());
			if (book.getRentPrice() == null) {
				throw new IllegalArgumentException("El libro " + book.getId() + " no tiene precio de renta definido");
			}

			RentDetail detail = new RentDetail();
			detail.setRent(saved);
			detail.setBook(book);
			detail.setMonthsRented(item.getMonthsRented());
			detail.setPrice(book.getRentPrice() * item.getMonthsRented());
			detail.setReturnDate(LocalDate.now().plusMonths(item.getMonthsRented()));
			detail.setExtensions(new ArrayList<>());
			saved.getRentDetails().add(detail);
			rentDetailRepository.save(detail);
		}

		long total = saved.getRentDetails().stream().mapToLong(RentDetail::getPrice).sum();
		saved.setTotalPrice((int) total);

		if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
			rentCouponService.applyCoupon(saved, request.getCouponCode());
		}
		return saved;
	}

	@Transactional
	public Rent updateUser(Long id, RentUpdateRequest request) {
		Rent rent = requireById(id);
		if (request.getUserId() == null) {
			rent.setUserAccount(null);
		} else {
			rent.setUserAccount(userAccountService.requireById(request.getUserId()));
		}
		return repository.save(rent);
	}

	@Transactional
	@Override
	public void delete(Long id) {
		Rent rent = requireById(id);
		rent.setDeleted(true);
		repository.save(rent);
	}

	public Page<Rent> list(Long userId, Long bookId, LocalDateTime createdFrom, LocalDateTime createdTo,
			Boolean overdue, String couponCode, int page, int size, String sort) {
		return repository.findAll(
				RentSpecifications.build(userId, bookId, createdFrom, createdTo, overdue, couponCode),
				buildPageable(page, size, sort));
	}

	private Pageable buildPageable(int page, int size, String sort) {
		Sort sortSpec = Sort.unsorted();
		if (sort != null && !sort.isBlank()) {
			String[] parts = sort.split(",");
			String field = parts[0].trim();
			Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
					? Sort.Direction.DESC
					: Sort.Direction.ASC;
			sortSpec = Sort.by(direction, field);
		}
		return PageRequest.of(Math.max(page, 0), Math.max(size, 1), sortSpec);
	}
}