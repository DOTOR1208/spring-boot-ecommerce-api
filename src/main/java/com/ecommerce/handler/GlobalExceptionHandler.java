package com.ecommerce.handler;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.ecommerce.exception.CartNotFoundException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.exception.UserAlreadyExistException;
import com.ecommerce.exception.UserNotFoundException;
import com.ecommerce.response.ErrorCode;
import com.ecommerce.response.ErrorResponse;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(UserNotFoundException.class)
    public ErrorResponse handleUserNotFound(final UserNotFoundException exception) {
        return ErrorResponse.of(exception.getErrorCode(), exception.getDomain(), exception.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ProductNotFoundException.class)
    public ErrorResponse handleProductNotFound(final ProductNotFoundException exception) {
        return ErrorResponse.of(exception.getErrorCode(), exception.getDomain(), exception.getMessage());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(CartNotFoundException.class)
    public ErrorResponse handleCartNotFound(final CartNotFoundException exception) {
        return ErrorResponse.of(exception.getErrorCode(), exception.getDomain(), exception.getMessage());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(UserAlreadyExistException.class)
    public ErrorResponse handleUserAlreadyExist(final UserAlreadyExistException exception) {
        return ErrorResponse.of(exception.getErrorCode(), exception.getDomain(), exception.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidation(final MethodArgumentNotValidException exception) {
        Map<String, Object> details = exception.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        x -> x.getField(),
                        x -> x.getDefaultMessage() != null ? x.getDefaultMessage() : "Invalid value",
                        (first, second) -> first
                ));

        return ErrorResponse.of(ErrorCode.INVALID, "request", "Validation failed", details);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public ErrorResponse handleConstraintViolation(final ConstraintViolationException exception) {
        Map<String, Object> details = exception.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        x -> x.getPropertyPath().toString(),
                        x -> x.getMessage(),
                        (first, second) -> first
                ));

        return ErrorResponse.of(ErrorCode.INVALID, "request", "Validation failed", details);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGenericException(final Exception exception) {
        return ErrorResponse.of(ErrorCode.INTERNAL_SERVER, "system", exception.getMessage());
    }
}
