package com.ecommerce.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.dto.request.CartRequestDto;
import com.ecommerce.dto.request.UpdateCartItemRequest;
import com.ecommerce.dto.response.CartResponseDto;
import com.ecommerce.handler.GlobalExceptionHandler;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.CartService;
import com.ecommerce.service.UserService;
import com.ecommerce.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private User sampleUser;
    private CustomUserDetails customUserDetails;
    private CartResponseDto sampleCartResponseDto;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().id(1).name("USER").build();
        this.sampleUser = User.builder()
                .id(1)
                .name("John Doe")
                .userName("johndoe")
                .email("john@example.com")
                .password("encoded_pass")
                .roles(List.of(role))
                .deleted(false)
                .build();

        this.customUserDetails = new CustomUserDetails(sampleUser);

        org.springframework.security.core.Authentication auth = 
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        customUserDetails, null, customUserDetails.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        this.sampleCartResponseDto = CartResponseDto.builder()
                .cartItems(Collections.emptyList())
                .totalPrice(0.0)
                .build();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/cart - When authenticated, returns 200 OK with user cart")
    void getCart_Success() throws Exception {
        when(userService.getUserEntityByUsername("johndoe")).thenReturn(sampleUser);
        when(cartService.getCart(sampleUser)).thenReturn(sampleCartResponseDto);

        mockMvc.perform(get("/api/cart")
                        .with(user(customUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalPrice").value(0.0));

        verify(cartService).getCart(sampleUser);
    }

    @Test
    @DisplayName("POST /api/cart/items - When valid request, adds item and returns 200 OK")
    void addToCart_Success() throws Exception {
        CartRequestDto request = CartRequestDto.builder()
                .productId(10)
                .quantity(2)
                .build();

        when(userService.getUserEntityByUsername("johndoe")).thenReturn(sampleUser);
        when(cartService.addToCart(eq(sampleUser), any(CartRequestDto.class))).thenReturn(sampleCartResponseDto);

        mockMvc.perform(post("/api/cart/items")
                        .with(user(customUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"));

        verify(cartService).addToCart(eq(sampleUser), any(CartRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/cart/items - When productId is null, returns 400 Bad Request")
    void addToCart_ValidationFailed_NullProductId() throws Exception {
        CartRequestDto invalidRequest = CartRequestDto.builder()
                .productId(null)
                .quantity(1)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .with(user(customUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.operationType").value("FAILURE"))
                .andExpect(jsonPath("$.code").value("INVALID"))
                .andExpect(jsonPath("$.details.productId").value("Product ID cannot be null"));

        verify(cartService, never()).addToCart(any(), any());
    }

    @Test
    @DisplayName("POST /api/cart/items - When quantity < 1, returns 400 Bad Request")
    void addToCart_ValidationFailed_InvalidQuantity() throws Exception {
        CartRequestDto invalidRequest = CartRequestDto.builder()
                .productId(1)
                .quantity(0)
                .build();

        mockMvc.perform(post("/api/cart/items")
                        .with(user(customUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.operationType").value("FAILURE"))
                .andExpect(jsonPath("$.code").value("INVALID"))
                .andExpect(jsonPath("$.details.quantity").value("Quantity must be at least 1"));

        verify(cartService, never()).addToCart(any(), any());
    }

    @Test
    @DisplayName("PUT /api/cart/items/{productId} - When valid quantity, updates and returns 200 OK")
    void updateCartItemQuantity_Success() throws Exception {
        UpdateCartItemRequest request = UpdateCartItemRequest.builder()
                .quantity(5)
                .build();

        when(userService.getUserEntityByUsername("johndoe")).thenReturn(sampleUser);
        when(cartService.updateCartItemQuantity(eq(sampleUser), eq(10), eq(5))).thenReturn(sampleCartResponseDto);

        mockMvc.perform(put("/api/cart/items/10")
                        .with(user(customUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"));

        verify(cartService).updateCartItemQuantity(sampleUser, 10, 5);
    }

    @Test
    @DisplayName("PUT /api/cart/items/{productId} - When quantity is negative, returns 400 Bad Request")
    void updateCartItemQuantity_ValidationFailed_NegativeQuantity() throws Exception {
        UpdateCartItemRequest invalidRequest = UpdateCartItemRequest.builder()
                .quantity(-2)
                .build();

        mockMvc.perform(put("/api/cart/items/10")
                        .with(user(customUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.operationType").value("FAILURE"))
                .andExpect(jsonPath("$.code").value("INVALID"))
                .andExpect(jsonPath("$.details.quantity").value("Quantity cannot be negative"));

        verify(cartService, never()).updateCartItemQuantity(any(), any(), any());
    }

    @Test
    @DisplayName("DELETE /api/cart/items/{productId} - Removes item from cart and returns 200 OK")
    void removeProductFromCart_Success() throws Exception {
        when(userService.getUserEntityByUsername("johndoe")).thenReturn(sampleUser);
        when(cartService.removeProductFromCart(sampleUser, 10)).thenReturn(sampleCartResponseDto);

        mockMvc.perform(delete("/api/cart/items/10")
                        .with(user(customUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"));

        verify(cartService).removeProductFromCart(sampleUser, 10);
    }

    @Test
    @DisplayName("DELETE /api/cart - Clears user cart and returns 200 OK")
    void clearCart_Success() throws Exception {
        when(userService.getUserEntityByUsername("johndoe")).thenReturn(sampleUser);
        doNothing().when(cartService).clearCart(sampleUser);

        mockMvc.perform(delete("/api/cart")
                        .with(user(customUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                .andExpect(jsonPath("$.data").value("Cart cleared successfully"));

        verify(cartService).clearCart(sampleUser);
    }
}
