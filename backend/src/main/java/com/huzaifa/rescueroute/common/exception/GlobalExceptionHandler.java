package com.huzaifa.rescueroute.common.exception;

import com.huzaifa.rescueroute.common.response.ApiResponse;
import com.huzaifa.rescueroute.common.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles all application exceptions globally.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle resource not found exceptions.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFound(
            ResourceNotFoundException ex) {

        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * Handle bean validation errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ValidationError>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        List<ValidationError> errors = new ArrayList<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {

            errors.add(
                    new ValidationError(
                            fieldError.getField(),
                            fieldError.getDefaultMessage()));
        }

        ApiResponse<List<ValidationError>> response = ApiResponse.error("Validation failed", errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle illegal argument / validation exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(
            IllegalArgumentException ex) {

        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle state conflict exceptions (e.g. attempting to re-process an already verified/rejected request).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalState(
            IllegalStateException ex) {

        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    /**
     * Handle missing required request parameters.
     */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParams(
            org.springframework.web.bind.MissingServletRequestParameterException ex) {

        ApiResponse<Object> response = ApiResponse.error("Required parameter '" + ex.getParameterName() + "' is missing");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle parameter type mismatch (e.g. string passed when Double expected).
     */
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {

        ApiResponse<Object> response = ApiResponse.error("Invalid value for parameter '" + ex.getName() + "'");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }


    /**
     * Handle authentication credentials failure.
     */
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentials(
            org.springframework.security.authentication.BadCredentialsException ex) {

        ApiResponse<Object> response = ApiResponse.error("Invalid email or password.");

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response);
    }

    /**
     * Handle deactivated user account.
     */
    @ExceptionHandler(org.springframework.security.authentication.DisabledException.class)
    public ResponseEntity<ApiResponse<Object>> handleDisabledAccount(
            org.springframework.security.authentication.DisabledException ex) {

        ApiResponse<Object> response = ApiResponse.error(ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    /**
     * Handle authorization access denied.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {

        ApiResponse<Object> response = ApiResponse.error("Forbidden: You do not have permission to access this resource");

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response);
    }

    /**
     * Handle unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception ex) {

        log.error("Unhandled exception processing request: ", ex);
        ApiResponse<Object> response = ApiResponse.error("An unexpected error occurred: " + ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}