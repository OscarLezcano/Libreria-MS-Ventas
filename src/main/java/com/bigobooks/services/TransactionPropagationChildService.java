package com.bigobooks.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Servicio secundario para demostrar como cada propagacion responde cuando se
 * invoca directamente y cuando ya existe una transaccion padre.
 */
@Service
public class TransactionPropagationChildService {

	private static final Logger log = LoggerFactory.getLogger(TransactionPropagationChildService.class);

	@Transactional(propagation = Propagation.REQUIRED)
	public String required() {
		return record("REQUIRED");
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public String requiresNew() {
		return record("REQUIRES_NEW");
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public String supports() {
		return record("SUPPORTS");
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public String notSupported() {
		return record("NOT_SUPPORTED");
	}

	@Transactional(propagation = Propagation.NEVER)
	public String never() {
		return record("NEVER");
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public String mandatory() {
		return record("MANDATORY");
	}

	private String record(String propagation) {
		boolean active = TransactionSynchronizationManager.isActualTransactionActive();
		String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
		String result = propagation + " -> active=" + active + ", name=" + transactionName;
		log.info("TX-DEMO {}", result);
		return result;
	}
}
