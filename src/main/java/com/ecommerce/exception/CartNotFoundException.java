package com.ecommerce.exception;

import com.ecommerce.response.ErrorCode;

import lombok.Getter;

@Getter
public class CartNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode = ErrorCode.NOT_FOUND;
    private final String domain = "cart";

    public CartNotFoundException(final String message) {
        super(message);
    }
}
