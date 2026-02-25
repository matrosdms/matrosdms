/*
 * Copyright (c) 2026 Matrosdms
 * This program is dual-licensed under:
 * GNU Affero General Public License (AGPL v3) - Open Source, Copyleft.
 * Commercial License - Proprietary, Closed Source.
 * See the LICENSE file for full details.
 */
package net.schwehla.matrosdms.config;

import java.io.IOException;
import java.net.ConnectException;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import net.schwehla.matrosdms.domain.api.ApiErrorResponse;
import net.schwehla.matrosdms.domain.api.EErrorCode;
import net.schwehla.matrosdms.exception.EntityNotFoundException;
import net.schwehla.matrosdms.exception.MatrosServiceException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * Handle SSE/Async Disconnects. We do NOT return a ResponseEntity because the
	 * connection is
	 * already broken.
	 */
	@ExceptionHandler(AsyncRequestNotUsableException.class)
	public void handleAsyncError(AsyncRequestNotUsableException ex) {
		// WARN: User closed tab while stream was open
		log.warn("SSE Client disconnected (Async timeout/closed): {}", ex.getMessage());
	}

	/** Handle Broken Pipes (Client killed connection during download/stream) */
	@ExceptionHandler(IOException.class)
	public void handleIOError(IOException ex) {
		// Filter out "Broken pipe" or "Connection reset"
		if (ex.getMessage() != null
				&& (ex.getMessage().contains("Broken pipe")
						|| ex.getMessage().contains("Connection reset")
						|| ex.getMessage().contains("abgebrochen"))) { // German Locale Support

			// WARN: Log single line, no stack trace
			log.warn("Client disconnected during IO (Broken pipe/Reset): {}", ex.getMessage());
		} else {
			// ERROR: Genuine I/O error (Disk full, etc)
			log.error("IO Exception: ", ex);
		}
	}

	// 503 Service Unavailable
	@ExceptionHandler({ ConnectException.class })
	public ResponseEntity<ApiErrorResponse> handleServiceUnavailable(
			Exception ex, HttpServletRequest request) {
		log.error("Service Unavailable: {}", ex.getMessage());
		return buildErrorResponse(
				HttpStatus.SERVICE_UNAVAILABLE,
				EErrorCode.INTERNAL_SERVER_ERROR,
				"Service Unavailable. Please check database or AI connection.",
				request);
	}

	// 409 Conflict
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleDbConstraint(
			DataIntegrityViolationException ex, HttpServletRequest request) {
		String msg = ex.getMostSpecificCause().getMessage().toLowerCase();
		if (msg.contains("sha256") || msg.contains("unique_id")) {
			return buildErrorResponse(
					HttpStatus.CONFLICT, EErrorCode.DUPLICATE_FILE, "File already exists.", request);
		}
		return buildErrorResponse(
				HttpStatus.CONFLICT,
				EErrorCode.DB_DUPLICATE_ENTRY,
				"Database constraint violation",
				request);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ApiErrorResponse> handleOptimisticLock(
			OptimisticLockingFailureException ex, HttpServletRequest request) {
		return buildErrorResponse(
				HttpStatus.CONFLICT,
				EErrorCode.DB_LOCK_CONCURRENT,
				"Data modified by another user.",
				request);
	}

	// 400 Bad Request
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidationErrors(
			MethodArgumentNotValidException ex, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(
				HttpStatus.BAD_REQUEST.value(),
				EErrorCode.VALIDATION_FAILED,
				HttpStatus.BAD_REQUEST.getReasonPhrase(),
				"Validation failed",
				request.getRequestURI());
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			response.addValidationError(error.getField(), error.getDefaultMessage());
		}
		return buildErrorResponse(HttpStatus.BAD_REQUEST, response);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
			HttpMessageNotReadableException ex, HttpServletRequest request) {
		
		Throwable root = ex.getRootCause(); 
		log.error("JSON parse error: {}", root != null ? root.getMessage() : ex.getMessage(), ex);
		
		return buildErrorResponse(
				HttpStatus.BAD_REQUEST, EErrorCode.REQ_JSON_MALFORMED, "Invalid JSON format", request);
	}

	// 404 Not Found
	@ExceptionHandler({ NoResourceFoundException.class, EntityNotFoundException.class })
	public ResponseEntity<ApiErrorResponse> handleNotFound(Exception ex, HttpServletRequest request) {
		return buildErrorResponse(
				HttpStatus.NOT_FOUND, EErrorCode.ENTITY_NOT_FOUND, ex.getMessage(), request);
	}

	// 422 Business Logic
	@ExceptionHandler(MatrosServiceException.class)
	public ResponseEntity<ApiErrorResponse> handleServiceException(
			MatrosServiceException ex, HttpServletRequest request) {
		return buildErrorResponse(
				HttpStatus.UNPROCESSABLE_ENTITY, EErrorCode.PROCESSING_ERROR, ex.getMessage(), request);
	}

	// ResponseStatusException (propagate intended status code)
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatus(
			ResponseStatusException ex, HttpServletRequest request) {
		HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
		log.warn("ResponseStatus {}: {} [{}]", status.value(), ex.getReason(), request.getRequestURI());
		return buildErrorResponse(
				status,
				EErrorCode.ENTITY_NOT_FOUND,
				ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(),
				request);
	}

	// 500 General
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleGeneralError(
			Exception ex, HttpServletRequest request) {
		log.error("Unhandled Exception: ", ex);
		return buildErrorResponse(
				HttpStatus.INTERNAL_SERVER_ERROR,
				EErrorCode.INTERNAL_SERVER_ERROR,
				"Server Error",
				request);
	}

	private ResponseEntity<ApiErrorResponse> buildErrorResponse(
			HttpStatus status, EErrorCode code, String message, HttpServletRequest request) {
		ApiErrorResponse response = new ApiErrorResponse(
				status.value(), code, status.getReasonPhrase(), message, request.getRequestURI());
		return buildErrorResponse(status, response);
	}

	private ResponseEntity<ApiErrorResponse> buildErrorResponse(
			HttpStatus status, ApiErrorResponse response) {
		return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(response);
	}
}
