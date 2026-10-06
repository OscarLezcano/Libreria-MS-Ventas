package com.bigobooks.mappers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.bigobooks.dto.PromotionDto;
import com.bigobooks.entities.book.Book;
import com.bigobooks.model.Promotion;

/**
 * Conversion bidireccional entre la entidad propia Promotion y su DTO.
 */
@Component
public class PromotionMapper {

	public PromotionDto toDto(Promotion promotion) {
		if (promotion == null) {
			return null;
		}
		PromotionDto dto = new PromotionDto();
		dto.setId(promotion.getId());
		dto.setName(promotion.getName());
		dto.setDiscountPercent(promotion.getDiscountPercent());
		dto.setStartDate(promotion.getStartDate());
		dto.setEndDate(promotion.getEndDate());
		dto.setCreatedAt(promotion.getCreatedAt());

		List<Long> bookIds = new ArrayList<>();
		if (promotion.getBooks() != null) {
			for (Book book : promotion.getBooks()) {
				bookIds.add(book.getId());
			}
		}
		dto.setBookIds(bookIds);
		return dto;
	}
}
