package com.bigobooks.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bigobooks.dto.PromotionBookAdd;
import com.bigobooks.dto.PromotionCreateRequest;
import com.bigobooks.entities.book.Book;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.model.Promotion;
import com.bigobooks.repositories.PromotionRepository;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

	@Mock
	private PromotionRepository repository;
	@Mock
	private BookService bookService;

	@InjectMocks
	private PromotionService promotionService;

	@Test
	void createAsociaLosLibros() {
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		Book book = new Book();
		book.setId(10L);
		when(bookService.requireById(10L)).thenReturn(book);

		PromotionCreateRequest request = new PromotionCreateRequest();
		request.setName("Semana del libro");
		request.setDiscountPercent(20);
		request.setStartDate(LocalDate.of(2026, 3, 1));
		request.setEndDate(LocalDate.of(2026, 3, 7));
		request.setBookIds(List.of(10L));

		Promotion promotion = promotionService.createFromRequest(request);

		assertEquals("Semana del libro", promotion.getName());
		assertEquals(20, promotion.getDiscountPercent());
		assertEquals(1, promotion.getBooks().size());
		assertEquals(book, promotion.getBooks().get(0));
	}

	@Test
	void createRechazaFechasInvertidas() {
		PromotionCreateRequest request = new PromotionCreateRequest();
		request.setName("Semana del libro");
		request.setDiscountPercent(20);
		request.setStartDate(LocalDate.of(2026, 3, 7));
		request.setEndDate(LocalDate.of(2026, 3, 1));

		assertThrows(IllegalArgumentException.class, () -> promotionService.createFromRequest(request));
	}

	@Test
	void createValidaElPorcentaje() {
		PromotionCreateRequest request = new PromotionCreateRequest();
		request.setName("Semana del libro");
		request.setDiscountPercent(0);
		request.setStartDate(LocalDate.of(2026, 3, 1));
		request.setEndDate(LocalDate.of(2026, 3, 7));

		assertThrows(IllegalArgumentException.class, () -> promotionService.createFromRequest(request));
	}

	@Test
	void addBooksNoDuplicaLibrosYaAsociados() {
		Book book = new Book();
		book.setId(10L);
		Promotion promotion = promotion();
		promotion.setBooks(new ArrayList<>(List.of(book)));
		when(repository.findById(1L)).thenReturn(Optional.of(promotion));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(bookService.requireById(10L)).thenReturn(book);

		PromotionBookAdd add = new PromotionBookAdd();
		add.setBookIds(List.of(10L));

		Promotion result = promotionService.addBooks(1L, add);

		assertEquals(1, result.getBooks().size());
	}

	@Test
	void removeBookDeUnaPromocionSinEseLibro() {
		Promotion promotion = promotion();
		promotion.setBooks(new ArrayList<>());
		when(repository.findById(1L)).thenReturn(Optional.of(promotion));

		assertThrows(NotFoundException.class, () -> promotionService.removeBook(1L, 10L));
	}

	private Promotion promotion() {
		Promotion promotion = new Promotion();
		promotion.setId(1L);
		promotion.setName("Semana del libro");
		promotion.setDiscountPercent(20);
		promotion.setStartDate(LocalDate.of(2026, 3, 1));
		promotion.setEndDate(LocalDate.of(2026, 3, 7));
		return promotion;
	}
}
