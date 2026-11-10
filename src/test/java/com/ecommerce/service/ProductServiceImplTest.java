package com.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    // ==========================================
    // 1. Tests for getAllProducts
    // ==========================================

    @Test
    @DisplayName("getAllProducts: Should return list of ProductResponseDto")
    void getAllProducts_ShouldReturnListOfProductResponseDto() {
        // Arrange
        List<Product> productList = List.of(sampleProduct);
        List<ProductResponseDto> dtoList = List.of(sampleResponseDto);

        when(productRepository.findAll()).thenReturn(productList);
        when(productMapper.toDTOs(productList)).thenReturn(dtoList);

        // Act
        List<ProductResponseDto> actual = productService.getAllProducts();

        // Assert
        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals("MacBook Pro M3", actual.get(0).getName());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllProducts(Pageable): Should return paged ProductResponseDto")
    void getAllProducts_WithPageable_ShouldReturnPagedResponseDto() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(sampleProduct), pageable, 1);

        when(productRepository.findAll(pageable)).thenReturn(productPage);
        when(productMapper.toDTO(sampleProduct)).thenReturn(sampleResponseDto);

        // Act
        Page<ProductResponseDto> actualPage = productService.getAllProducts(pageable);

        // Assert
        assertNotNull(actualPage);
        assertEquals(1, actualPage.getTotalElements());
        assertEquals(1, actualPage.getContent().size());
        assertEquals("MacBook Pro M3", actualPage.getContent().get(0).getName());
        verify(productRepository, times(1)).findAll(pageable);
    }

    // ==========================================
    // 2. Tests for getProductById
    // ==========================================

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

    // ==========================================
    // 3. Tests for createProduct
    // ==========================================

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

        verify(productRepository, times(1)).existsByItemCode(1001);
        verify(productRepository, never()).save(any(Product.class));
    }

    // ==========================================
    // 4. Tests for updateProduct
    // ==========================================

    @Test
    @DisplayName("updateProduct: Should update and return DTO when request is valid")
    void updateProduct_WhenValid_ShouldUpdateAndReturnDto() {
        // Arrange: Update with same itemCode (1001) or new unique itemCode
        ProductRequestDto updateRequest = ProductRequestDto.builder()
                .name("MacBook Pro M3 Max")
                .itemCode(1001)
                .price(2500.0)
                .stock(15)
                .build();

        ProductResponseDto updatedDto = ProductResponseDto.builder()
                .id(1)
                .name("MacBook Pro M3 Max")
                .itemCode(1001)
                .price(2500.0)
                .stock(15)
                .build();

        when(productRepository.findById(1)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(sampleProduct)).thenReturn(sampleProduct);
        when(productMapper.toDTO(sampleProduct)).thenReturn(updatedDto);

        // Act
        ProductResponseDto actual = productService.updateProduct(1, updateRequest);

        // Assert
        assertNotNull(actual);
        assertEquals("MacBook Pro M3 Max", actual.getName());
        assertEquals(2500.0, actual.getPrice());
        verify(productMapper, times(1)).updateEntityFromDto(updateRequest, sampleProduct);
        verify(productRepository, times(1)).save(sampleProduct);
    }

    @Test
    @DisplayName("updateProduct: Should throw ProductNotFoundException when product does not exist")
    void updateProduct_WhenProductNotFound_ShouldThrowProductNotFoundException() {
        // Arrange
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> {
            productService.updateProduct(999, sampleRequestDto);
        });
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("updateProduct: Should throw BadRequestException when changing to an existing itemCode")
    void updateProduct_WhenNewItemCodeAlreadyExists_ShouldThrowBadRequestException() {
        // Arrange: Changing itemCode from 1001 to 2002, but 2002 already exists in DB
        ProductRequestDto updateRequest = ProductRequestDto.builder()
                .name("MacBook Pro M3")
                .itemCode(2002)
                .price(2000.0)
                .stock(10)
                .build();

        when(productRepository.findById(1)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.existsByItemCode(2002)).thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            productService.updateProduct(1, updateRequest);
        });
        verify(productRepository, never()).save(any(Product.class));
    }

    // ==========================================
    // 5. Tests for deleteProduct
    // ==========================================

    @Test
    @DisplayName("deleteProduct: Should delete product when product exists")
    void deleteProduct_WhenProductExists_ShouldDeleteProduct() {
        // Arrange
        when(productRepository.findById(1)).thenReturn(Optional.of(sampleProduct));

        // Act
        productService.deleteProduct(1);

        // Assert
        verify(productRepository, times(1)).findById(1);
        verify(productRepository, times(1)).delete(sampleProduct);
    }

    @Test
    @DisplayName("deleteProduct: Should throw ProductNotFoundException when product does not exist")
    void deleteProduct_WhenProductNotFound_ShouldThrowProductNotFoundException() {
        // Arrange
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> {
            productService.deleteProduct(999);
        });
        verify(productRepository, times(1)).findById(999);
        verify(productRepository, never()).delete(any(Product.class));
    }

    // ==========================================
    // 6. Tests for searchProducts
    // ==========================================

    @Test
    @DisplayName("searchProducts: Should return paged ProductResponseDto matching search criteria")
    void searchProducts_ShouldReturnPagedResponseDto() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> matchingPage = new PageImpl<>(List.of(sampleProduct), pageable, 1);

        when(productRepository.findByNameContainingIgnoreCase("MacBook", pageable)).thenReturn(matchingPage);
        when(productMapper.toDTO(sampleProduct)).thenReturn(sampleResponseDto);

        // Act
        Page<ProductResponseDto> actualPage = productService.searchProducts("MacBook", pageable);

        // Assert
        assertNotNull(actualPage);
        assertFalse(actualPage.isEmpty());
        assertEquals(1, actualPage.getTotalElements());
        assertEquals("MacBook Pro M3", actualPage.getContent().get(0).getName());
        verify(productRepository, times(1)).findByNameContainingIgnoreCase("MacBook", pageable);
    }
}
