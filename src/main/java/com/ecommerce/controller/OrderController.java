package com.ecommerce.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.response.OrderDetailResponseDto;
import com.ecommerce.dto.response.OrderResponseDto;
import com.ecommerce.model.User;
import com.ecommerce.response.SuccessResponse;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.OrderService;
import com.ecommerce.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    @PostMapping("/checkout")
    public SuccessResponse<OrderResponseDto> checkoutOrder(@AuthenticationPrincipal final CustomUserDetails userDetails) {
        log.info("Received checkout request for user: '{}'", userDetails.getUsername());
        User user = this.userService.getUserEntityByUsername(userDetails.getUsername());
        return SuccessResponse.of(this.orderService.checkoutOrder(user));
    }

    @GetMapping
    public SuccessResponse<List<OrderResponseDto>> getOrderSummaries(@AuthenticationPrincipal final CustomUserDetails userDetails) {
        log.info("Received request to get order summaries for user: '{}'", userDetails.getUsername());
        User user = this.userService.getUserEntityByUsername(userDetails.getUsername());
        return SuccessResponse.of(this.orderService.getOrderSummaries(user));
    }

    @GetMapping("/{orderId}")
    public SuccessResponse<OrderDetailResponseDto> getOrderDetail(
            @PathVariable final Integer orderId,
            @AuthenticationPrincipal final CustomUserDetails userDetails) {
        log.info("Received request to get order detail for order ID {} from user '{}'",
                orderId, userDetails.getUsername());
        User user = this.userService.getUserEntityByUsername(userDetails.getUsername());
        return SuccessResponse.of(this.orderService.getOrderDetail(orderId, user));
    }
}
