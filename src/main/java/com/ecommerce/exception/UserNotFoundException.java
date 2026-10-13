package com.ecommerce.exception;

public class UserNotFoundException extends ResourceNotFoundException {

    private static final long serialVersionUID = 1L;

    public UserNotFoundException(final String message) {
        super("user", message);
    }
}
