package com.ecommerce.exception;

import com.ecommerce.response.ErrorCode;

public abstract class ResourceNotFoundException extends BaseException {

    private static final long serialVersionUID = 1L;

    protected ResourceNotFoundException(final String domain, final String message) {
        super(ErrorCode.NOT_FOUND, domain, message);
    }
}
