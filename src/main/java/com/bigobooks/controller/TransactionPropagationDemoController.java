package com.bigobooks.controller;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bigobooks.services.TransactionPropagationChildService;
import com.bigobooks.services.TransactionPropagationParentService;

/**
 * Endpoints didacticos para demostrar propagacion de transacciones. No
 * participan en el flujo real de ventas.
 */
@RestController
@RequestMapping("/transaction-demo")
public class TransactionPropagationDemoController {

	private final TransactionPropagationChildService childService;
	private final TransactionPropagationParentService parentService;

	public TransactionPropagationDemoController(TransactionPropagationChildService childService,
			TransactionPropagationParentService parentService) {
		this.childService = childService;
		this.parentService = parentService;
	}

	@GetMapping("/direct")
	public ResponseEntity<Map<String, Object>> direct() {
		List<String> results = new ArrayList<>();
		results.add(invoke("REQUIRED", childService::required));
		results.add(invoke("REQUIRES_NEW", childService::requiresNew));
		results.add(invoke("SUPPORTS", childService::supports));
		results.add(invoke("NOT_SUPPORTED", childService::notSupported));
		results.add(invoke("NEVER", childService::never));
		results.add(invoke("MANDATORY", childService::mandatory));
		return ResponseEntity.ok(Map.of("scenario", "direct-child-call", "results", results));
	}

	@GetMapping("/from-parent")
	public ResponseEntity<Map<String, Object>> fromParent() {
		List<String> results = parentService.parentCalls();
		return ResponseEntity.ok(Map.of("scenario", "parent-calls-child", "results", results));
	}

	private String invoke(String propagation, java.util.function.Supplier<String> call) {
		try {
			return call.get();
		} catch (RuntimeException exception) {
			return propagation + " -> EXCEPTION: " + exception.getClass().getSimpleName();
		}
	}
}
