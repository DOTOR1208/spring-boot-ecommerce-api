package com.ecommerce.dto.response;

import java.time.LocalDateTime;
import java.util.List;

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
public class OrderDetailResponseDto {

    private Integer orderId;
    private LocalDateTime orderDate;
    private String status;
    private List<OrderItemResponseDto> items;
    private Double totalPrice;
}
