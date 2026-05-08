package com.linkforge.urlshortener.exception.handler;

import com.linkforge.urlshortener.exception.auth.InvalidCredentialsException;
import com.linkforge.urlshortener.exception.auth.TokenException;
import com.linkforge.urlshortener.exception.auth.UnauthorizedException;
import com.linkforge.urlshortener.exception.input.InvalidAliasException;
import com.linkforge.urlshortener.exception.input.InvalidExportFormatException;
import com.linkforge.urlshortener.exception.input.InvalidRequestException;
import com.linkforge.urlshortener.exception.resource.DuplicateResourceException;
import com.linkforge.urlshortener.exception.resource.ResourceNotFoundException;
import com.linkforge.urlshortener.exception.url.ClickLimitExceededException;
import com.linkforge.urlshortener.exception.url.LinkNotActiveException;
import com.linkforge.urlshortener.exception.url.ShortCodeGenerationException;
import com.linkforge.urlshortener.exception.url.UrlExpiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Handles every exception thrown across all controllers and returns standardized JSON error responses
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ==========================================
    // Resource exceptions
    // ==========================================

    // 404 - Short URL or user not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", null);
    }

    // 409 - Duplicate email, username, or custom alias
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateResource(DuplicateResourceException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), "DUPLICATE_RESOURCE", null);
    }

    // 409 - DB-level UNIQUE constraint violation (e.g. concurrent short code collision)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, "A resource with the same unique value already exists", "DUPLICATE_RESOURCE", null);
    }

    // ==========================================
    // URL-specific exceptions
    // ==========================================

    // 410 - URL is deactivated (is_active = false)
    @ExceptionHandler(LinkNotActiveException.class)
    public ResponseEntity<Map<String, Object>> handleLinkNotActive(LinkNotActiveException ex) {
        return buildErrorResponse(HttpStatus.GONE, ex.getMessage(), "LINK_INACTIVE", null);
    }

    // 410 - URL has passed its expiration date
    @ExceptionHandler(UrlExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleUrlExpired(UrlExpiredException ex) {
        return buildErrorResponse(HttpStatus.GONE, ex.getMessage(), "LINK_EXPIRED", null);
    }

    // 410 - URL has reached its maximum click count
    @ExceptionHandler(ClickLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleClickLimitExceeded(ClickLimitExceededException ex) {
        return buildErrorResponse(HttpStatus.GONE, ex.getMessage(), "CLICK_LIMIT_EXCEEDED", null);
    }

    // ==========================================
    // Validation and input exceptions
    // ==========================================

    // 400 - Invalid alias format or reserved alias word
    @ExceptionHandler(InvalidAliasException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidAlias(InvalidAliasException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_ALIAS", null);
    }

    // 400 - Invalid export format (only 'json' and 'csv' are supported)
    @ExceptionHandler(InvalidExportFormatException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidExportFormat(InvalidExportFormatException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_EXPORT_FORMAT", null);
    }

    // 500 - All short code generation retries exhausted
    @ExceptionHandler(ShortCodeGenerationException.class)
    public ResponseEntity<Map<String, Object>> handleShortCodeGeneration(ShortCodeGenerationException ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), "SHORT_CODE_GENERATION_FAILED", null);
    }

    // 400 - General invalid request (bad URL format, past expiry date, unsafe URL, invalid sortBy, etc.)
    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(InvalidRequestException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_REQUEST", null);
    }

    // 400 - Bean validation failed on a @RequestBody field.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fieldErrors = buildFieldErrors(ex.getBindingResult().getAllErrors());
        return buildValidationErrorResponse(fieldErrors);
    }

    // 400 - @RequestParam or @ModelAttribute binding/validation failure.
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(BindException ex) {
        List<Map<String, String>> fieldErrors = buildFieldErrors(ex.getBindingResult().getAllErrors());
        return buildValidationErrorResponse(fieldErrors);
    }

    // 400 - Required query parameter is missing entirely (e.g. missing ?format= on export)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
        String message = "Required parameter '" + ex.getParameterName() + "' is missing";
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, "MISSING_PARAMETER", null);
    }

    // 400 - Path variable or request param has the wrong type (e.g. "abc" instead of a numeric ID)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, "INVALID_PARAMETER", null);
    }

    // 400 - Request body is missing, empty, or not valid JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Request body is missing or malformed", "INVALID_REQUEST_BODY", null);
    }

    // 415 - Client sent a Content-Type the endpoint does not support (e.g. text/plain instead of application/json)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        String message = "Content type '" + ex.getContentType() + "' is not supported. Use 'application/json'";
        return buildErrorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message, "UNSUPPORTED_MEDIA_TYPE", null);
    }

    // 406 - Client sent an Accept header the API cannot satisfy
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Map<String, Object>> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        return buildErrorResponse(HttpStatus.NOT_ACCEPTABLE, "Requested media type is not supported by this endpoint", "NOT_ACCEPTABLE", null);
    }

    // ==========================================
    // Authentication exceptions
    // ==========================================

    // 401 - Wrong login credentials or wrong current password
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), "INVALID_CREDENTIALS", null);
    }

    // 401 - Refresh token is expired, revoked, or not found
    @ExceptionHandler(TokenException.class)
    public ResponseEntity<Map<String, Object>> handleTokenException(TokenException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), "TOKEN_INVALID", null);
    }

    // 401 - No authenticated user in security context, or user deleted after token was issued
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), "UNAUTHORIZED", null);
    }

    // 401 - Spring Security blocked an unauthenticated request (fallback for filter-level auth failures)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Authentication required", "AUTHENTICATION_REQUIRED", null);
    }

    // ==========================================
    // Authorization exceptions
    // ==========================================

    // 403 - Authenticated user tried to access a resource they do not own
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, "You do not have permission to perform this action", "ACCESS_DENIED", null);
    }

    // ==========================================
    // HTTP / routing exceptions
    // ==========================================

    // 405 - HTTP method not allowed on this endpoint
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        String message = "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint";
        return buildErrorResponse(HttpStatus.METHOD_NOT_ALLOWED, message, "METHOD_NOT_ALLOWED", null);
    }

    // 404 - Endpoint does not exist at all
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoResourceFound(NoResourceFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, "The requested endpoint does not exist", "ENDPOINT_NOT_FOUND", null);
    }

    // ==========================================
    // Fallback
    // ==========================================

    // 500 - Any unexpected exception not caught above
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                "INTERNAL_SERVER_ERROR", null);
    }

    // ==========================================
    // Helpers
    // ==========================================

    // Builds a standardized error response envelope.
    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status,
                                                                    String message,
                                                                    String errorCode,
                                                                    String errorDetails) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", status.value());
        response.put("success", false);
        response.put("message", message);

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", errorCode);
        error.put("details", errorDetails);
        response.put("error", error);

        response.put("timestamp", LocalDateTime.now());
        return new ResponseEntity<>(response, status);
    }

    // Builds a validation error response with field-level errors nested inside the error object.
    private ResponseEntity<Map<String, Object>> buildValidationErrorResponse(List<Map<String, String>> fieldErrors) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("success", false);
        response.put("message", "Validation failed");

        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", "VALIDATION_FAILED");
        error.put("details", null);
        error.put("errors", fieldErrors);
        response.put("error", error);

        response.put("timestamp", LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // Extracts field-level error details from a binding result.
    private List<Map<String, String>> buildFieldErrors(List<ObjectError> allErrors) {
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        for (ObjectError objectError : allErrors) {
            Map<String, String> fieldError = new LinkedHashMap<>();
            if (objectError instanceof FieldError fe) {
                fieldError.put("field", fe.getField());
            } else {
                fieldError.put("field", objectError.getObjectName());
            }
            fieldError.put("message", objectError.getDefaultMessage());
            fieldErrors.add(fieldError);
        }
        return fieldErrors;
    }
}
