package com.bigobooks.services;

import com.bigobooks.entities.rents.RentExtension;
import com.bigobooks.repositories.RentExtensionRepository;

import org.springframework.stereotype.Service;

/**
 * CRUD base de extensiones de renta (la logica de la extension la aplica
 * {@link RentDetailService}).
 */
@Service
public class RentExtensionService extends BaseService<RentExtension, Long, RentExtensionRepository> {

	public RentExtensionService(RentExtensionRepository repository) {
		super(repository);
	}
}