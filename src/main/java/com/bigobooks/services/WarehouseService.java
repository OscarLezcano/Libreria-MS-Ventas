package com.bigobooks.services;

import org.springframework.stereotype.Service;

import com.bigobooks.entities.stock.Warehouse;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.repositories.WarehouseRepository;
import com.bigobooks.service.BaseService;

/**
 * Servicio de apoyo (sin controlador): validacion de almacenes.
 */
@Service
public class WarehouseService extends BaseService<Warehouse, WarehouseRepository> {

	public WarehouseService(WarehouseRepository repository) {
		super(repository);
	}

	public Warehouse requireById(Long id) {
		return findById(id).orElseThrow(() -> new NotFoundException("No existe el almacen con id " + id));
	}
}
