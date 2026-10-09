package com.ecommerce.exception;

import com.ecommerce.response.ErrorCode;

import lombok.Getter;

@Getter
public class UserNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode = ErrorCode.NOT_FOUND;
    private final String domain = "user";

    public UserNotFoundException(final String message) {
        super(message);
    }
}
