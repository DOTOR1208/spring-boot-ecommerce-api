package com.ecommerce.exception;

import com.ecommerce.response.ErrorCode;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;
    private final String domain;

    protected BaseException(final ErrorCode errorCode, final String domain, final String message) {
        super(message);
        this.errorCode = errorCode;
        this.domain = domain;
    }
}
