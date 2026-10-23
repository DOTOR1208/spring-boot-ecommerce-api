package com.ecommerce.service;

import com.ecommerce.dto.request.CartRequestDto;
import com.ecommerce.dto.response.CartResponseDto;
import com.ecommerce.model.User;

public interface CartService {

    CartResponseDto getCart(User user);

    CartResponseDto addToCart(User user, CartRequestDto request);

    CartResponseDto removeProductFromCart(User user, Integer productId);

    void clearCart(User user);
}
