package com.ecommerce.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.dto.request.ProductRequestDto;
import com.ecommerce.dto.response.ProductResponseDto;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    public List<ProductResponseDto> getAllProducts() {
        log.info("Fetching all products");
        return this.productMapper.toDTOs(this.productRepository.findAll());
    }

    @Override
    public Page<ProductResponseDto> getAllProducts(final Pageable pageable) {
        log.info("Fetching products with pagination: {}", pageable);
        return this.productRepository.findAll(pageable)
                .map(this.productMapper::toDTO);
    }

    @Override
    public ProductResponseDto getProductById(final Integer id) {
        log.info("Fetching product by ID: {}", id);
        Product product = this.productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(String.format("Product not found with id: %d", id)));
        return this.productMapper.toDTO(product);
    }

    @Override
    @Transactional
    public ProductResponseDto createProduct(final ProductRequestDto request) {
        log.info("Creating product with name: '{}', itemCode: {}", request.getName(), request.getItemCode());

        if (this.productRepository.existsByItemCode(request.getItemCode())) {
            log.warn("Create product failed: itemCode {} already exists", request.getItemCode());
            throw new BadRequestException(String.format("Product with itemCode '%d' already exists", request.getItemCode()));
        }

        Product product = this.productMapper.toModel(request);
        Product savedProduct = this.productRepository.save(product);
        log.info("Product created successfully with ID: {}", savedProduct.getId());

        return this.productMapper.toDTO(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponseDto updateProduct(final Integer id, final ProductRequestDto request) {
        log.info("Updating product with ID: {}", id);

        Product product = this.productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(String.format("Product not found with id: %d", id)));

        if (!product.getItemCode().equals(request.getItemCode()) && this.productRepository.existsByItemCode(request.getItemCode())) {
            log.warn("Update product failed: itemCode {} already exists", request.getItemCode());
            throw new BadRequestException(String.format("Product with itemCode '%d' already exists", request.getItemCode()));
        }

        this.productMapper.updateEntityFromDto(request, product);
        Product updatedProduct = this.productRepository.save(product);
        log.info("Product with ID: {} updated successfully", id);

        return this.productMapper.toDTO(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(final Integer id) {
        log.info("Deleting product with ID: {}", id);

        Product product = this.productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(String.format("Product not found with id: %d", id)));

        this.productRepository.delete(product);
        log.info("Product with ID: {} deleted successfully", id);
    }

    @Override
    public Page<ProductResponseDto> searchProducts(final String name, final Pageable pageable) {
        log.info("Searching products by name '{}' with pagination: {}", name, pageable);
        return this.productRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(this.productMapper::toDTO);
    }
}
