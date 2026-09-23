package com.bigobooks.mappers;

import org.springframework.stereotype.Component;

import com.bigobooks.dto.RentDetailDto;
import com.bigobooks.entities.rents.RentDetail;

@Component
public class RentDetailMapper {

	public RentDetailDto toDto(RentDetail detail) {
		RentDetailDto dto = new RentDetailDto();
		dto.setId(detail.getId());
		dto.setBookId(detail.getBook() != null ? detail.getBook().getId() : null);
		dto.setPrice(detail.getPrice());
		dto.setMonthsRented(detail.getMonthsRented());
		dto.setReturnDate(detail.getReturnDate());
		dto.setRentId(detail.getRent() != null ? detail.getRent().getId() : null);
		return dto;
	}
}