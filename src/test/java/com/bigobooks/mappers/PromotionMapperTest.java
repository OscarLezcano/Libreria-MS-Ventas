package com.bigobooks.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bigobooks.dto.PromotionDto;
import com.bigobooks.entities.book.Book;
import com.bigobooks.model.Promotion;

/**
 * Conversion bidireccional Promocion &lt;-&gt; DTO (Entrega #2).
 */
class PromotionMapperTest {

	private final PromotionMapper mapper = new PromotionMapper();

	@Test
	void convierteEntidadADto() {
		Book book = new Book();
		book.setId(10L);

		Promotion promotion = new Promotion();
		promotion.setId(4L);
		promotion.setName("Semana del libro");
		promotion.setDiscountPercent(20);
		promotion.setStartDate(LocalDate.of(2026, 10, 1));
		promotion.setEndDate(LocalDate.of(2026, 10, 7));
		promotion.setBooks(List.of(book));

		PromotionDto dto = mapper.toDto(promotion);

		assertEquals(4L, dto.getId());
		assertEquals("Semana del libro", dto.getName());
		assertEquals(20, dto.getDiscountPercent());
		assertEquals(LocalDate.of(2026, 10, 1), dto.getStartDate());
		assertEquals(LocalDate.of(2026, 10, 7), dto.getEndDate());
		assertEquals(List.of(10L), dto.getBookIds());
	}

	@Test
	void convierteDtoAEntidad() {
		PromotionDto dto = new PromotionDto();
		dto.setId(4L);
		dto.setName("Semana del libro");
		dto.setDiscountPercent(20);
		dto.setStartDate(LocalDate.of(2026, 10, 1));
		dto.setEndDate(LocalDate.of(2026, 10, 7));
		dto.setBookIds(List.of(10L));

		Promotion promotion = mapper.toEntity(dto);

		assertEquals(4L, promotion.getId());
		assertEquals("Semana del libro", promotion.getName());
		assertEquals(20, promotion.getDiscountPercent());
		assertEquals(LocalDate.of(2026, 10, 1), promotion.getStartDate());
		assertEquals(LocalDate.of(2026, 10, 7), promotion.getEndDate());
	}

	@Test
	void elRoundtripConservaLosCamposPlanos() {
		Promotion promotion = new Promotion();
		promotion.setId(4L);
		promotion.setName("Semana del libro");
		promotion.setDiscountPercent(20);
		promotion.setStartDate(LocalDate.of(2026, 10, 1));
		promotion.setEndDate(LocalDate.of(2026, 10, 7));

		PromotionDto dto = mapper.toDto(mapper.toEntity(mapper.toDto(promotion)));

		assertEquals(promotion.getId(), dto.getId());
		assertEquals(promotion.getName(), dto.getName());
		assertEquals(promotion.getDiscountPercent(), dto.getDiscountPercent());
		assertEquals(promotion.getStartDate(), dto.getStartDate());
		assertEquals(promotion.getEndDate(), dto.getEndDate());
	}

	@Test
	void convierteValoresNulos() {
		assertNull(mapper.toDto(null));
		assertNull(mapper.toEntity(null));
	}
}
