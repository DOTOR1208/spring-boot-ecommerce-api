package com.ecommerce.handler;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.BaseException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.exception.UserAlreadyExistException;
import com.ecommerce.response.ErrorCode;
import com.ecommerce.response.ErrorResponse;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles all ResourceNotFoundException subclasses (UserNotFound, ProductNotFound, CartNotFound, etc.)
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ErrorResponse handleResourceNotFound(final ResourceNotFoundException exception) {
        log.warn("Resource not found [{}]: {}", exception.getDomain(), exception.getMessage());
        return ErrorResponse.of(exception.getErrorCode(), exception.getDomain(), exception.getMessage());
    }

    /**
     * Handles duplicate resource conflicts
     */
    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(UserAlreadyExistException.class)
    public ErrorResponse handleUserAlreadyExist(final UserAlreadyExistException exception) {
        log.warn("Conflict detected [{}]: {}", exception.getDomain(), exception.getMessage());
        return ErrorResponse.of(exception.getErrorCode(), exception.getDomain(), exception.getMessage());
    }

    /**
     * Handles any custom domain exceptions extending BaseException
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BaseException.class)
    public ErrorResponse handleBaseException(final BaseException exception) {
        log.warn("Business exception [{}]: {}", exception.getDomain(), exception.getMessage());
        return ErrorResponse.of(exception.getErrorCode(), exception.getDomain(), exception.getMessage());
    }

    /**
     * Handles DTO validation errors (@Valid)
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidation(final MethodArgumentNotValidException exception) {
        Map<String, Object> details = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String defaultMessage = fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value";
            details.put(fieldError.getField(), defaultMessage);
        });

        log.warn("DTO validation failed: {}", details);
        return ErrorResponse.of(ErrorCode.INVALID, "request", "Validation failed", details);
    }

    /**
     * Handles JPA/Hibernate constraint violations
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ErrorResponse handleConstraintViolation(final ConstraintViolationException exception) {
        Map<String, Object> details = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
            details.put(violation.getPropertyPath().toString(), violation.getMessage())
        );

        log.warn("Constraint violation: {}", details);
        return ErrorResponse.of(ErrorCode.INVALID, "request", "Validation failed", details);
    }

    /**
     * Handles malformed or unreadable JSON bodies
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ErrorResponse handleHttpMessageNotReadable(final HttpMessageNotReadableException exception) {
        log.warn("Malformed HTTP request body: {}", exception.getMessage());
        return ErrorResponse.of(ErrorCode.INVALID, "request", "Malformed JSON request or invalid field format");
    }

    /**
     * Handles URI parameter type mismatch (e.g. passing a string for an integer id)
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ErrorResponse handleMethodArgumentTypeMismatch(final MethodArgumentTypeMismatchException exception) {
        String requiredType = exception.getRequiredType() != null ? exception.getRequiredType().getSimpleName() : "unknown";
        String message = String.format("Parameter '%s' should be of type '%s'", exception.getName(), requiredType);
        log.warn("Type mismatch: {}", message);
        return ErrorResponse.of(ErrorCode.INVALID, "request", message);
    }

    /**
     * Handles endpoint / route not found (Spring Boot 3)
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public ErrorResponse handleNoResourceFound(final NoResourceFoundException exception) {
        log.warn("Static or endpoint route not found: {}", exception.getResourcePath());
        return ErrorResponse.of(ErrorCode.NOT_FOUND, "routing", "Endpoint not found: " + exception.getResourcePath());
    }

    /**
     * Handles HTTP method not supported (e.g. POST on GET endpoint)
     */
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ErrorResponse handleMethodNotSupported(final HttpRequestMethodNotSupportedException exception) {
        log.warn("Method not supported: {}", exception.getMethod());
        return ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED, "request", exception.getMessage());
    }

    /**
     * Handles Spring Security AccessDeniedException at controller / method level (@PreAuthorize)
     */
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ErrorResponse handleAccessDenied(final AccessDeniedException exception) {
        log.warn("Access denied at controller level: {}", exception.getMessage());
        return ErrorResponse.of(
                ErrorCode.FORBIDDEN,
                "auth",
                "Access denied. You do not have permission to access this resource."
        );
    }

    /**
     * Handles authentication failures (wrong password, disabled or locked account)
     */
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({
            BadCredentialsException.class,
            DisabledException.class,
            LockedException.class,
            AuthenticationException.class
    })
    public ErrorResponse handleAuthenticationException(final AuthenticationException exception) {
        log.warn("Authentication failed: {}", exception.getMessage());
        String message = exception instanceof BadCredentialsException
                ? "Invalid username or password"
                : exception.getMessage();
        return ErrorResponse.of(ErrorCode.UNAUTHORIZED, "auth", message);
    }

    /**
     * Catch-all for unexpected internal server errors
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGenericException(final Exception exception) {
        log.error("Unhandled server exception: ", exception);
        String message = exception.getMessage() != null && !exception.getMessage().isBlank()
                ? exception.getMessage()
                : "An unexpected error occurred";
        return ErrorResponse.of(ErrorCode.INTERNAL_SERVER, "system", message);
    }
}
