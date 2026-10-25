package com.ecommerce.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.request.ProductRequestDto;
import com.ecommerce.dto.response.ProductResponseDto;
import com.ecommerce.response.SuccessResponse;
import com.ecommerce.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public SuccessResponse<?> getProducts(final Pageable pageable) {
        log.info("Received request to get products with pageable: {}", pageable);
        if (pageable.isPaged()) {
            return SuccessResponse.of(this.productService.getAllProducts(pageable));
        }
        return SuccessResponse.of(this.productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public SuccessResponse<ProductResponseDto> getProductById(@PathVariable final Integer id) {
        log.info("Received request to get product by ID: {}", id);
        return SuccessResponse.of(this.productService.getProductById(id));
    }

    @GetMapping("/search")
    public SuccessResponse<?> searchProducts(
            @RequestParam final String name,
            final Pageable pageable) {
        log.info("Received request to search products by name '{}'", name);
        return SuccessResponse.of(this.productService.searchProducts(name, pageable));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SuccessResponse<ProductResponseDto> createProduct(@Valid @RequestBody final ProductRequestDto request) {
        log.info("Received request to create product: {}", request.getName());
        return SuccessResponse.of(this.productService.createProduct(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SuccessResponse<ProductResponseDto> updateProduct(
            @PathVariable final Integer id,
            @Valid @RequestBody final ProductRequestDto request) {
        log.info("Received request to update product ID: {}", id);
        return SuccessResponse.of(this.productService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SuccessResponse<String> deleteProduct(@PathVariable final Integer id) {
        log.info("Received request to delete product ID: {}", id);
        this.productService.deleteProduct(id);
        return SuccessResponse.of("Product deleted successfully");
    }
}
