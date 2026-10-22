package com.ecommerce.exception;

public class OrderNotFoundException extends ResourceNotFoundException {

    private static final long serialVersionUID = 1L;

    public OrderNotFoundException(final String message) {
        super("order", message);
    }

    public OrderNotFoundException(final Long orderId) {
        super("Order", "id", orderId);
    }
}
