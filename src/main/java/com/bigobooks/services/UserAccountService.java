package com.bigobooks.services;

import org.springframework.stereotype.Service;

import com.bigobooks.entities.auth.UserAccount;
import com.bigobooks.util.exception.NotFoundException;
import com.bigobooks.util.exception.ResourceType;
import com.bigobooks.repositories.UserAccountRepository;
import com.bigobooks.service.BaseService;

/**
 * Servicio de apoyo (sin controlador): acceso a los usuarios/compradores.
 */
@Service
public class UserAccountService extends BaseService<UserAccount, UserAccountRepository> {

	public UserAccountService(UserAccountRepository repository) {
		super(repository);
	}

	public UserAccount requireById(Long id) {
		return findById(id).orElseThrow(() -> NotFoundException.byId(ResourceType.USER, id));
	}
}
