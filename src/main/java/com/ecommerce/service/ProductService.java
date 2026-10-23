package com.ecommerce.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.ecommerce.dto.request.ProductRequestDto;
import com.ecommerce.dto.response.ProductResponseDto;

public interface ProductService {

    List<ProductResponseDto> getAllProducts();

    Page<ProductResponseDto> getAllProducts(Pageable pageable);

    ProductResponseDto getProductById(Integer id);

    ProductResponseDto createProduct(ProductRequestDto request);

    ProductResponseDto updateProduct(Integer id, ProductRequestDto request);

    void deleteProduct(Integer id);

    Page<ProductResponseDto> searchProducts(String name, Pageable pageable);
}
