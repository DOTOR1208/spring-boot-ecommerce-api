package com.ecommerce.exception;

import com.ecommerce.response.ErrorCode;

import lombok.Getter;

@Getter
public class UserAlreadyExistException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode = ErrorCode.ALREADY_EXIST;
    private final String domain = "user";

    public UserAlreadyExistException(final String message) {
        super(message);
    }
}
