package com.ecommerce.dto.response;

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
public class ProductResponseDto {

    private Integer id;
    private String name;
    private String description;
    private Integer itemCode;
    private String manufacturer;
    private String category;
    private Integer stock;
    private Double price;
}
