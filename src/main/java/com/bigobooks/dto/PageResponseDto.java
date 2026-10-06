package com.bigobooks.dto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Envoltura de paginacion. Este DTO no se genera desde openapi (el common no
 * lo incluye); se define aqui a mano, igual que ApiErrorDto, y la lista
 * content se llena con el DTO correspondiente de cada endpoint.
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponseDto {

	private List<Object> content;
	private Integer page;
	private Integer size;
	private Long totalElements;
	private Integer totalPages;

	public static PageResponseDto from(Page<?> source) {
		PageResponseDto dto = new PageResponseDto();
		dto.setContent(new ArrayList<>(source.getContent()));
		dto.setPage(source.getNumber());
		dto.setSize(source.getSize());
		dto.setTotalElements(source.getTotalElements());
		dto.setTotalPages(source.getTotalPages());
		return dto;
	}
}
