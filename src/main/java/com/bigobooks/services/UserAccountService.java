package com.bigobooks.services;

import com.bigobooks.entities.auth.UserAccount;
import com.bigobooks.exception.NotFoundException;
import com.bigobooks.repositories.UserAccountRepository;

import org.springframework.stereotype.Service;

/**
 * Acceso a usuarios (entidad del common gestionada por otro microservicio).
 * Solo lectura: este servicio no crea ni modifica usuarios.
 */
@Service
public class UserAccountService extends BaseService<UserAccount, Long, UserAccountRepository> {

	public UserAccountService(UserAccountRepository repository) {
		super(repository);
	}

	public UserAccount requireById(Long id) {
		try {
			return getById(id);
		} catch (IllegalArgumentException e) {
			throw new NotFoundException("No existe el usuario con id " + id);
		}
	}
}