package com.bigobooks.mappers;

import org.springframework.stereotype.Component;

import com.bigobooks.dto.UserAccountDto;
import com.bigobooks.entities.auth.UserAccount;

/**
 * Conversion de UserAccount a su DTO del common (no expone el hash).
 */
@Component
public class UserAccountMapper {

	public UserAccountDto toDto(UserAccount user) {
		if (user == null) {
			return null;
		}
		UserAccountDto dto = new UserAccountDto();
		dto.setId(user.getId());
		dto.setName(user.getName());
		dto.setLastName(user.getLastName());
		dto.setEmail(user.getEmail());
		dto.setCity(user.getCity());
		dto.setWarehouseId(user.getWarehouseId());
		dto.setCreatedAt(user.getCreatedAt());
		dto.setRoleId(user.getRole() != null ? user.getRole().getId() : null);
		return dto;
	}
}
