package com.ecommerce.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.dto.response.OrderDetailResponseDto;
import com.ecommerce.dto.response.OrderResponseDto;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.OrderNotFoundException;
import com.ecommerce.handler.GlobalExceptionHandler;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.UserService;
import com.ecommerce.utils.JwtUtils;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private User sampleUser;
    private CustomUserDetails customUserDetails;
    private OrderResponseDto sampleOrderResponseDto;
    private OrderDetailResponseDto sampleOrderDetailResponseDto;

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

        this.sampleOrderResponseDto = OrderResponseDto.builder()
                .orderId(100)
                .orderDate(LocalDateTime.now())
                .status("COMPLETED")
                .totalPrice(250.0)
                .build();

        this.sampleOrderDetailResponseDto = OrderDetailResponseDto.builder()
                .orderId(100)
                .orderDate(LocalDateTime.now())
                .status("COMPLETED")
                .items(Collections.emptyList())
                .totalPrice(250.0)
                .build();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/orders/checkout - Checkouts user cart and returns 200 OK")
    void checkoutOrder_Success() throws Exception {
        when(userService.getUserEntityByUsername("johndoe")).thenReturn(sampleUser);
        when(orderService.checkoutOrder(sampleUser)).thenReturn(sampleOrderResponseDto);

        mockMvc.perform(post("/api/orders/checkout")
                        .with(user(customUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                .andExpect(jsonPath("$.data.orderId").value(100))
                .andExpect(jsonPath("$.data.totalPrice").value(250.0));

        verify(orderService).checkoutOrder(sampleUser);
    }

    @Test
    @DisplayName("POST /api/orders/checkout - When cart is empty, returns 400 Bad Request")
    void checkoutOrder_EmptyCart_Returns400() throws Exception {
        when(userService.getUserEntityByUsername("johndoe")).thenReturn(sampleUser);
        when(orderService.checkoutOrder(sampleUser))
                .thenThrow(new BadRequestException("Cart is empty, cannot checkout"));

        mockMvc.perform(post("/api/orders/checkout")
                        .with(user(customUserDetails)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.operationType").value("FAILURE"))
                .andExpect(jsonPath("$.message").value("Cart is empty, cannot checkout"));
    }

    @Test
    @DisplayName("GET /api/orders - Returns list of user order summaries")
    void getOrderSummaries_Success() throws Exception {
        when(userService.getUserEntityByUsername("johndoe")).thenReturn(sampleUser);
        when(orderService.getOrderSummaries(sampleUser)).thenReturn(List.of(sampleOrderResponseDto));

        mockMvc.perform(get("/api/orders")
                        .with(user(customUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].orderId").value(100));

        verify(orderService).getOrderSummaries(sampleUser);
    }

    @Test
    @DisplayName("GET /api/orders/{orderId} - When order exists, returns 200 OK with details")
    void getOrderDetail_Success() throws Exception {
        when(userService.getUserEntityByUsername("johndoe")).thenReturn(sampleUser);
        when(orderService.getOrderDetail(100, sampleUser)).thenReturn(sampleOrderDetailResponseDto);

        mockMvc.perform(get("/api/orders/100")
                        .with(user(customUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.operationType").value("SUCCESS"))
                .andExpect(jsonPath("$.data.orderId").value(100));

        verify(orderService).getOrderDetail(100, sampleUser);
    }

    @Test
    @DisplayName("GET /api/orders/{orderId} - When order not found, returns 404 Not Found")
    void getOrderDetail_NotFound_Returns404() throws Exception {
        when(userService.getUserEntityByUsername("johndoe")).thenReturn(sampleUser);
        when(orderService.getOrderDetail(999, sampleUser))
                .thenThrow(new OrderNotFoundException("Order not found with id: 999"));

        mockMvc.perform(get("/api/orders/999")
                        .with(user(customUserDetails)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.operationType").value("FAILURE"))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Order not found with id: 999"));
    }
}
