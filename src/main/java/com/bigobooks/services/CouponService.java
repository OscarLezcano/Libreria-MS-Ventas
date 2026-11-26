package com.bigobooks.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigobooks.cache.CacheNames;
import com.bigobooks.dto.CouponCreateRequest;
import com.bigobooks.dto.CouponUpdateRequest;
import com.bigobooks.entities.book.Book;
import com.bigobooks.entities.orders.Coupon;
import com.bigobooks.util.exception.ConflictException;
import com.bigobooks.util.exception.NotFoundException;
import com.bigobooks.util.exception.ResourceType;
import com.bigobooks.repositories.CouponRepository;
import com.bigobooks.service.BaseService;
import com.bigobooks.specifications.CouponSpecifications;

@Service
public class CouponService extends BaseService<Coupon, CouponRepository> {

	private static final Logger log = LoggerFactory.getLogger(CouponService.class);

	private final BookService bookService;

	public CouponService(CouponRepository repository, BookService bookService) {
		super(repository);
		this.bookService = bookService;
	}

	public Coupon requireById(Long id) {
		return findById(id).orElseThrow(() -> NotFoundException.byId(ResourceType.COUPON, id));
	}

	public Coupon requireByCode(String code) {
		return getRepository().findByCode(code.trim())
				.orElseThrow(() -> NotFoundException.byCode(ResourceType.COUPON, code));
	}

	@Transactional
	public Coupon createFromRequest(CouponCreateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Debe indicar los datos del cupon");
		}
		String code = request.getCode() != null ? request.getCode().trim() : "";
		int discountPercent = requirePercent(request.getDiscountPercent());
		if (code.isEmpty()) {
			throw new IllegalArgumentException("El codigo del cupon es obligatorio");
		}
		validateDates(request.getValidFrom(), request.getValidUntil());
		if (getRepository().existsByCode(code)) {
			throw new ConflictException("Ya existe un cupon con el codigo " + code);
		}

		Coupon coupon = new Coupon();
		coupon.setCode(code);
		coupon.setDiscountPercent(discountPercent);
		coupon.setValidFrom(request.getValidFrom());
		coupon.setValidUntil(request.getValidUntil());
		coupon.setSingleUsePerUser(Boolean.TRUE.equals(request.getSingleUsePerUser()));
		coupon.setBooks(resolveBooks(request.getBookIds()));
		Coupon saved = getRepository().save(coupon);
		log.info("Cupon {} creado ({}% de descuento, {} libro(s), vence {})", saved.getCode(),
				saved.getDiscountPercent(), saved.getBooks().size(), saved.getValidUntil());
		return saved;
	}

	@CacheEvict(cacheNames = CacheNames.COUPONS, key = "#id")
	@Transactional
	public Coupon update(Long id, CouponUpdateRequest request) {
		if (request == null) {
			throw new IllegalArgumentException("Debe indicar los datos del cupon");
		}
		Coupon coupon = requireById(id);
		validateDates(request.getValidFrom(), request.getValidUntil());
		coupon.setDiscountPercent(requirePercent(request.getDiscountPercent()));
		coupon.setValidFrom(request.getValidFrom());
		coupon.setValidUntil(request.getValidUntil());
		if (request.getSingleUsePerUser() != null) {
			coupon.setSingleUsePerUser(request.getSingleUsePerUser());
		}
		if (request.getBookIds() != null) {
			coupon.setBooks(resolveBooks(request.getBookIds()));
		}
		Coupon saved = getRepository().save(coupon);
		log.info("Cupon {} actualizado ({}%)", saved.getCode(), saved.getDiscountPercent());
		return saved;
	}

	public Page<Coupon> list(String code, Boolean active, Integer minDiscountPercent, Integer page, Integer size,
			String sort) {
		Page<Coupon> result = getRepository().findAll(
				CouponSpecifications.build(code, active, minDiscountPercent), Pageables.of(page, size, sort));
		log.debug("Listado de cupones: pagina {} con {} resultado(s)", result.getNumber(), result.getTotalElements());
		return result;
	}

	@CacheEvict(cacheNames = CacheNames.COUPONS, key = "#id")
	@Transactional
	public void delete(Long id) {
		Coupon coupon = requireById(id);
		coupon.setDeleted(true);
		getRepository().save(coupon);
		log.info("Cupon {} eliminado (borrado logico)", coupon.getCode());
	}

	private List<Book> resolveBooks(List<Long> bookIds) {
		Set<Long> uniqueIds = new LinkedHashSet<>();
		if (bookIds != null) {
			for (Long bookId : bookIds) {
				if (bookId == null) {
					throw new IllegalArgumentException("bookIds no puede contener null");
				}
				uniqueIds.add(bookId);
			}
		}
		List<Book> books = new ArrayList<>();
		for (Long bookId : uniqueIds) {
			books.add(bookService.requireById(bookId));
		}
		return books;
	}

	private void validateDates(LocalDateTime validFrom, LocalDateTime validUntil) {
		if (validFrom != null && validUntil != null && validFrom.isAfter(validUntil)) {
			throw new IllegalArgumentException("validFrom no puede ser posterior a validUntil");
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
