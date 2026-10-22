package com.ecommerce.exception;

import com.ecommerce.response.ErrorCode;

public class BadRequestException extends BaseException {

    private static final long serialVersionUID = 1L;

    public BadRequestException(final String message) {
        super(ErrorCode.INVALID, "request", message);
    }

    public BadRequestException(final String domain, final String message) {
        super(ErrorCode.INVALID, domain, message);
    }
}
