package com.ecommerce.exception;

public class CartNotFoundException extends ResourceNotFoundException {

    private static final long serialVersionUID = 1L;

    public CartNotFoundException(final String message) {
        super("cart", message);
    }
}
