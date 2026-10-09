package com.bigobooks.exception;

/**
 * Credenciales invalidas (login). Se traduce a HTTP 401.
 */
public class UnauthorizedException extends RuntimeException {

	public UnauthorizedException(String message) {
		super(message);
	}
}
