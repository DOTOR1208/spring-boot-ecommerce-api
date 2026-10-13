package com.ecommerce.exception;

import com.ecommerce.response.ErrorCode;

public class UserAlreadyExistException extends BaseException {

    private static final long serialVersionUID = 1L;

    public UserAlreadyExistException(final String message) {
        super(ErrorCode.ALREADY_EXIST, "user", message);
    }
}
