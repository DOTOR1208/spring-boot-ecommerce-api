package com.ecommerce.response;

import java.time.OffsetDateTime;
import java.util.Collection;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SuccessResponse<T> {

    @Builder.Default
    private OperationType operationType = OperationType.SUCCESS;

    @Builder.Default
    private String message = "success";

    @Builder.Default
    private ErrorCode code = ErrorCode.OK;

    private T data;

    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    private int size;
    private int page;

    private Integer totalPages;
    private Long totalElements;

    public static <T> SuccessResponse<T> of(final T data) {
        SuccessResponseBuilder<T> builder = SuccessResponse.<T>builder()
                .data(data)
                .code(ErrorCode.OK);

        if (data instanceof Page<?>) {
            Page<?> pageData = (Page<?>) data;
            builder.size(pageData.getNumberOfElements())
                   .page(pageData.getNumber())
                   .totalPages(pageData.getTotalPages())
                   .totalElements(pageData.getTotalElements());
        } else if (data instanceof Collection<?>) {
            builder.size(((Collection<?>) data).size())
                   .page(0);
        } else {
            builder.size(data != null ? 1 : 0)
                   .page(0);
        }

        return builder.build();
    }

    public static <T> SuccessResponse<T> of(final T data, final int page) {
        SuccessResponse<T> response = of(data);
        response.setPage(page);
        return response;
    }
}
