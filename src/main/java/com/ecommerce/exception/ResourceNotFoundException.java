package com.ecommerce.exception;

import com.ecommerce.response.ErrorCode;

public class ResourceNotFoundException extends BaseException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(final String domain, final String message) {
        super(ErrorCode.NOT_FOUND, domain, message);
    }

    public ResourceNotFoundException(final String message) {
        super(ErrorCode.NOT_FOUND, "resource", message);
    }

    public ResourceNotFoundException(final String resourceName, final String fieldName, final Object fieldValue) {
        super(ErrorCode.NOT_FOUND, resourceName.toLowerCase(),
                String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
