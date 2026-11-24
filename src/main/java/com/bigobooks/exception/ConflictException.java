package com.bigobooks.exception;

/**
 * Operacion no permitida en el estado actual del recurso (HTTP 409).
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
