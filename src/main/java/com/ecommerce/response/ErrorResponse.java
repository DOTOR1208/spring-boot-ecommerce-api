package com.ecommerce.response;

import java.time.OffsetDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

    @Builder.Default
    private OperationType operationType = OperationType.FAILURE;

    private String message;
    private ErrorCode code;
    private String domain;
    private Map<String, Object> details;

    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    public static ErrorResponse of(final ErrorCode errorCode, final String domain) {
        return ErrorResponse.builder()
                .code(errorCode)
                .domain(domain)
                .message("Exception occurred!")
                .build();
    }

    public static ErrorResponse of(final ErrorCode errorCode, final String domain, final String message) {
        return ErrorResponse.builder()
                .code(errorCode)
                .domain(domain)
                .message(message)
                .build();
    }

    public static ErrorResponse of(final ErrorCode errorCode, final String domain, final String message, final Map<String, Object> details) {
        return ErrorResponse.builder()
                .code(errorCode)
                .domain(domain)
                .message(message)
                .details(details)
                .build();
    }
}
