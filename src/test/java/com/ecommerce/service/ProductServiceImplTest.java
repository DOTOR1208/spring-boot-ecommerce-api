package com.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.dto.request.ProductRequestDto;
import com.ecommerce.dto.response.ProductResponseDto;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.mapper.ProductMapper;
import com.ecommerce.model.Product;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.impl.ProductServiceImpl;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product sampleProduct;
    private ProductResponseDto sampleResponseDto;
    private ProductRequestDto sampleRequestDto;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1)
                .name("MacBook Pro M3")
                .itemCode(1001)
                .price(2000.0)
                .stock(10)
                .build();

        sampleResponseDto = ProductResponseDto.builder()
                .id(1)
                .name("MacBook Pro M3")
                .itemCode(1001)
                .price(2000.0)
                .stock(10)
                .build();

        sampleRequestDto = ProductRequestDto.builder()
                .name("MacBook Pro M3")
                .itemCode(1001)
                .price(2000.0)
                .stock(10)
                .build();
    }

    @Test
    @DisplayName("getProductById: Should return ProductResponseDto when product exists")
    void getProductById_WhenProductExists_ShouldReturnProductResponseDto() {
        // Arrange
        when(productRepository.findById(1)).thenReturn(Optional.of(sampleProduct));
        when(productMapper.toDTO(sampleProduct)).thenReturn(sampleResponseDto);

        // Act
        ProductResponseDto actual = productService.getProductById(1);

        // Assert
        assertNotNull(actual);
        assertEquals(1, actual.getId());
        assertEquals("MacBook Pro M3", actual.getName());
        verify(productRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("getProductById: Should throw ProductNotFoundException when product does not exist")
    void getProductById_WhenProductNotFound_ShouldThrowProductNotFoundException() {
        // Arrange
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> {
            productService.getProductById(999);
        });
        verify(productRepository, times(1)).findById(999);
    }

    @Test
    @DisplayName("createProduct: Should save and return DTO when itemCode is unique")
    void createProduct_WhenItemCodeIsUnique_ShouldSaveAndReturnDto() {
        // Arrange
        when(productRepository.existsByItemCode(1001)).thenReturn(false);
        when(productMapper.toModel(sampleRequestDto)).thenReturn(sampleProduct);
        when(productRepository.save(sampleProduct)).thenReturn(sampleProduct);
        when(productMapper.toDTO(sampleProduct)).thenReturn(sampleResponseDto);

        // Act
        ProductResponseDto actual = productService.createProduct(sampleRequestDto);

        // Assert
        assertNotNull(actual);
        assertEquals(1001, actual.getItemCode());
        verify(productRepository, times(1)).existsByItemCode(1001);
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("createProduct: Should throw BadRequestException when itemCode already exists")
    void createProduct_WhenItemCodeAlreadyExists_ShouldThrowBadRequestException() {
        // Arrange
        when(productRepository.existsByItemCode(1001)).thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            productService.createProduct(sampleRequestDto);
        });

        // Ensure save is never called when duplicate itemCode is detected
        verify(productRepository, times(1)).existsByItemCode(1001);
        verify(productRepository, never()).save(any(Product.class));
    }
}
