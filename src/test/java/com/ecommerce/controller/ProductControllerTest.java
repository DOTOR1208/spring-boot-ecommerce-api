package com.ecommerce.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.dto.request.ProductRequestDto;
import com.ecommerce.dto.response.ProductResponseDto;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.handler.GlobalExceptionHandler;
import com.ecommerce.service.ProductService;
import com.ecommerce.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@EnableMethodSecurity
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private ProductResponseDto sampleProductDto;
    private ProductRequestDto sampleProductRequest;

    @BeforeEach
    void setUp() {
        this.sampleProductDto = ProductResponseDto.builder()
                .id(1)
                .name("MacBook Pro")
                .description("Apple Silicon laptop")
                .itemCode(1001)
                .manufacturer("Apple")
                .category("Electronics")
                .stock(50)
                .price(1999.99)
                .build();

        this.sampleProductRequest = ProductRequestDto.builder()
                .name("MacBook Pro")
                .description("Apple Silicon laptop")
                .itemCode(1001)
                .manufacturer("Apple")
                .category("Electronics")
                .stock(50)
                .price(1999.99)
                .build();
    }

    @Test
    @DisplayName("GET /api/products - Public endpoint, returns 200 OK with product list")
    void getProducts_Success() throws Exception {
        Page<ProductResponseDto> page = new PageImpl<>(List.of(sampleProductDto));
        when(productService.getAllProducts(any(Pageable.class))).thenReturn(page);
        when(productService.getAllProducts()).thenReturn(List.of(sampleProductDto));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"));
    }

    @Test
    @DisplayName("GET /api/products/{id} - When product exists, returns 200 OK with product details")
    void getProductById_Success() throws Exception {
        when(productService.getProductById(1)).thenReturn(sampleProductDto);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("MacBook Pro"))
                .andExpect(jsonPath("$.data.price").value(1999.99));

        verify(productService).getProductById(1);
    }

    @Test
    @DisplayName("GET /api/products/{id} - When product not found, returns 404 Not Found")
    void getProductById_NotFound_Returns404() throws Exception {
        when(productService.getProductById(99))
                .thenThrow(new ProductNotFoundException("Product not found with id: 99"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.operationType").value("FAILURE"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"));

        verify(productService).getProductById(99);
    }

    @Test
    @DisplayName("GET /api/products/search - Public endpoint, returns matching products")
    void searchProducts_Success() throws Exception {
        Page<ProductResponseDto> page = new PageImpl<>(List.of(sampleProductDto));
        when(productService.searchProducts(eq("MacBook"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/products/search")
                        .param("name", "MacBook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                .andExpect(jsonPath("$.data.content[0].name").value("MacBook Pro"));

        verify(productService).searchProducts(eq("MacBook"), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/products - When user has ROLE_ADMIN and valid body, returns 200 OK")
    void createProduct_AsAdmin_Success() throws Exception {
        when(productService.createProduct(any(ProductRequestDto.class))).thenReturn(sampleProductDto);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleProductRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("MacBook Pro"));

        verify(productService).createProduct(any(ProductRequestDto.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("POST /api/products - When user has ROLE_USER, returns 403 Forbidden")
    void createProduct_AsUser_Returns403Forbidden() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleProductRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.operationType").value("FAILURE"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(productService, never()).createProduct(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/products - When body has validation errors, returns 400 Bad Request")
    void createProduct_ValidationFailed_Returns400() throws Exception {
        ProductRequestDto invalidRequest = ProductRequestDto.builder()
                .name("")
                .stock(-5)
                .price(-10.0)
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.operationType").value("FAILURE"))
                .andExpect(jsonPath("$.code").value("INVALID"))
                .andExpect(jsonPath("$.details.name").value("Product name cannot be blank"))
                .andExpect(jsonPath("$.details.itemCode").value("Item code cannot be null"))
                .andExpect(jsonPath("$.details.stock").value("Stock must be greater than or equal to 0"))
                .andExpect(jsonPath("$.details.price").value("Price must be greater than or equal to 0"));

        verify(productService, never()).createProduct(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/products/{id} - When user has ROLE_ADMIN, updates and returns 200 OK")
    void updateProduct_AsAdmin_Success() throws Exception {
        when(productService.updateProduct(eq(1), any(ProductRequestDto.class))).thenReturn(sampleProductDto);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleProductRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(productService).updateProduct(eq(1), any(ProductRequestDto.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/products/{id} - When user has ROLE_ADMIN, deletes and returns 200 OK")
    void deleteProduct_AsAdmin_Success() throws Exception {
        doNothing().when(productService).deleteProduct(1);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value("Product deleted successfully"));

        verify(productService).deleteProduct(1);
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("DELETE /api/products/{id} - When user has ROLE_USER, returns 403 Forbidden")
    void deleteProduct_AsUser_Returns403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.operationType").value("FAILURE"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verify(productService, never()).deleteProduct(any());
    }
}
