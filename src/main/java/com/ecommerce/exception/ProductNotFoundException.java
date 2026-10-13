package com.ecommerce.exception;

public class ProductNotFoundException extends ResourceNotFoundException {

    private static final long serialVersionUID = 1L;

    public ProductNotFoundException(final String message) {
        super("product", message);
    }
}
