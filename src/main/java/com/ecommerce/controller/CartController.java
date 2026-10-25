package com.ecommerce.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.dto.request.CartRequestDto;
import com.ecommerce.dto.response.CartResponseDto;
import com.ecommerce.model.User;
import com.ecommerce.response.SuccessResponse;
import com.ecommerce.security.CustomUserDetails;
import com.ecommerce.service.CartService;
import com.ecommerce.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @GetMapping
    public SuccessResponse<CartResponseDto> getCart(@AuthenticationPrincipal final CustomUserDetails userDetails) {
        log.info("Received request to get cart for user: '{}'", userDetails.getUsername());
        User user = this.userService.getUserEntityByUsername(userDetails.getUsername());
        return SuccessResponse.of(this.cartService.getCart(user));
    }

    @PostMapping("/items")
    public SuccessResponse<CartResponseDto> addToCart(
            @AuthenticationPrincipal final CustomUserDetails userDetails,
            @Valid @RequestBody final CartRequestDto request) {
        log.info("Received request to add product ID {} to cart for user '{}'",
                request.getProductId(), userDetails.getUsername());
        User user = this.userService.getUserEntityByUsername(userDetails.getUsername());
        return SuccessResponse.of(this.cartService.addToCart(user, request));
    }

    @DeleteMapping("/items/{productId}")
    public SuccessResponse<CartResponseDto> removeProductFromCart(
            @AuthenticationPrincipal final CustomUserDetails userDetails,
            @PathVariable final Integer productId) {
        log.info("Received request to remove product ID {} from cart for user '{}'",
                productId, userDetails.getUsername());
        User user = this.userService.getUserEntityByUsername(userDetails.getUsername());
        return SuccessResponse.of(this.cartService.removeProductFromCart(user, productId));
    }

    @DeleteMapping
    public SuccessResponse<String> clearCart(@AuthenticationPrincipal final CustomUserDetails userDetails) {
        log.info("Received request to clear cart for user '{}'", userDetails.getUsername());
        User user = this.userService.getUserEntityByUsername(userDetails.getUsername());
        this.cartService.clearCart(user);
        return SuccessResponse.of("Cart cleared successfully");
    }
}
