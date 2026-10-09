package com.ecommerce.response;

import java.time.OffsetDateTime;
import java.util.Collection;

import org.springframework.data.domain.Page;

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
public class SuccessResponse<T> {

    @Builder.Default
    private OperationType operationType = OperationType.SUCCESS;

    @Builder.Default
    private String message = "success";

    private ErrorCode code;
    private T data;

    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    private int size;
    private int page;

    public static <T> SuccessResponse<T> of(final T data) {
        return SuccessResponse.<T>builder()
                .data(data)
                .code(ErrorCode.OK)
                .size(getSize(data))
                .build();
    }

    public static <T> SuccessResponse<T> of(final T data, final int page) {
        return SuccessResponse.<T>builder()
                .data(data)
                .code(ErrorCode.OK)
                .size(getSize(data))
                .page(page)
                .build();
    }

    private static <T> int getSize(final T data) {
        if (data instanceof Collection<?>) {
            return ((Collection<?>) data).size();
        } else if (data instanceof Page<?>) {
            return ((Page<?>) data).getNumberOfElements();
        }
        return data != null ? 1 : 0;
    }
}
