package com.bigobooks.mappers;

import org.springframework.stereotype.Component;

import com.bigobooks.dto.RentExtensionDto;
import com.bigobooks.entities.rents.RentExtension;

@Component
public class RentExtensionMapper {

	public RentExtensionDto toDto(RentExtension extension) {
		RentExtensionDto dto = new RentExtensionDto();
		dto.setId(extension.getId());
		dto.setRentDetailId(extension.getRentDetail() != null ? extension.getRentDetail().getId() : null);
		dto.setMonthsExtended(extension.getMonthsExtended());
		dto.setExtraPrice(extension.getExtraPrice());
		dto.setNewReturnDate(extension.getNewReturnDate());
		return dto;
	}
}