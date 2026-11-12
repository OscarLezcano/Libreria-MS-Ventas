package com.bigobooks.services;

import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio principal que abre la transaccion inicial y delega en el servicio
 * secundario.
 */
@Service
public class TransactionPropagationParentService {

	private final TransactionPropagationChildService childService;

	public TransactionPropagationParentService(TransactionPropagationChildService childService) {
		this.childService = childService;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public List<String> parentCalls() {
		List<String> results = new ArrayList<>();
		results.add(invoke("REQUIRED", childService::required));
		results.add(invoke("REQUIRES_NEW", childService::requiresNew));
		results.add(invoke("SUPPORTS", childService::supports));
		results.add(invoke("NOT_SUPPORTED", childService::notSupported));
		results.add(invoke("NEVER", childService::never));
		results.add(invoke("MANDATORY", childService::mandatory));
		return results;
	}

	private String invoke(String propagation, java.util.function.Supplier<String> call) {
		try {
			return call.get();
		} catch (RuntimeException exception) {
			return propagation + " -> EXCEPTION: " + exception.getClass().getSimpleName();
		}
	}
}
