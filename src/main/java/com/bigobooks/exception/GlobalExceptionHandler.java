package com.bigobooks.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.bigobooks.dto.ApiErrorDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiErrorDto> handleNotFound(NotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiErrorDto> handleConflict(ConflictException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiErrorDto> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
		return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage(), request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorDto> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorDto> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<ApiErrorDto.FieldErrorDto> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new ApiErrorDto.FieldErrorDto(fe.getField(), fe.getDefaultMessage()))
				.toList();
		ApiErrorDto body = buildBody(HttpStatus.BAD_REQUEST, "Bad Request", "Error de validacion en el cuerpo",
				request);
		body.setFieldErrors(fieldErrors);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorDto> handleConstraintViolation(ConstraintViolationException ex,
			HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorDto> handleDataIntegrity(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, "Conflict", "Conflicto de integridad con los datos", request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorDto> handleGeneric(Exception ex, HttpServletRequest request) {
		log.error("Error no controlado en {}", request.getRequestURI(), ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
				"Error interno del servidor", request);
	}

	private ResponseEntity<ApiErrorDto> build(HttpStatus status, String error, String message,
			HttpServletRequest request) {
		return ResponseEntity.status(status).body(buildBody(status, error, message, request));
	}

	private ApiErrorDto buildBody(HttpStatus status, String error, String message, HttpServletRequest request) {
		ApiErrorDto dto = new ApiErrorDto();
		dto.setTimestamp(LocalDateTime.now());
		dto.setStatus(status.value());
		dto.setError(error);
		dto.setMessage(message);
		dto.setPath(request.getRequestURI());
		return dto;
	}
}