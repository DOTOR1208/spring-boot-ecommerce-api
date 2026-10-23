package com.ecommerce.service.impl;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.dto.request.CartRequestDto;
import com.ecommerce.dto.response.CartResponseDto;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.CartNotFoundException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.mapper.CartMapper;
import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.CartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;

    @Override
    public CartResponseDto getCart(final User user) {
        log.info("Fetching cart for user: '{}'", user.getUserName());
        Cart cart = this.cartRepository.findByUser(user).orElseGet(() -> {
            log.info("No cart found for user '{}', creating a new cart", user.getUserName());
            Cart newCart = Cart.builder().user(user).build();
            return this.cartRepository.save(newCart);
        });
        return this.cartMapper.toDTO(cart);
    }

    @Override
    @Transactional
    public CartResponseDto addToCart(final User user, final CartRequestDto request) {
        log.info("Adding product ID {} (quantity: {}) to cart for user '{}'",
                request.getProductId(), request.getQuantity(), user.getUserName());

        Cart cart = this.cartRepository.findByUser(user).orElseGet(() -> {
            log.info("No cart found for user '{}', creating a new cart", user.getUserName());
            Cart newCart = Cart.builder().user(user).build();
            return this.cartRepository.save(newCart);
        });

        Product product = this.productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(String.format("Product not found with id: %d", request.getProductId())));

        Optional<CartItem> existingItemOpt = cart.getCartItems().stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(product.getId()))
                .findFirst();

        int requestedTotal = request.getQuantity();
        if (existingItemOpt.isPresent()) {
            requestedTotal += existingItemOpt.get().getQuantity();
        }

        if (product.getStock() < requestedTotal) {
            log.warn("Insufficient stock for product ID {}. Available: {}, Requested: {}",
                    product.getId(), product.getStock(), requestedTotal);
            throw new BadRequestException(String.format("Insufficient stock for product '%s'. Available: %d, Requested: %d",
                    product.getName(), product.getStock(), requestedTotal));
        }

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            this.cartItemRepository.save(existingItem);
            log.info("Updated quantity for product ID {} to {} in cart ID {}",
                    product.getId(), existingItem.getQuantity(), cart.getId());
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getCartItems().add(newItem);
            this.cartItemRepository.save(newItem);
            log.info("Added new product ID {} with quantity {} to cart ID {}",
                    product.getId(), request.getQuantity(), cart.getId());
        }

        Cart updatedCart = this.cartRepository.save(cart);
        return this.cartMapper.toDTO(updatedCart);
    }

    @Override
    @Transactional
    public CartResponseDto removeProductFromCart(final User user, final Integer productId) {
        log.info("Removing product ID {} from cart for user '{}'", productId, user.getUserName());

        Cart cart = this.cartRepository.findByUser(user)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user"));

        CartItem itemToRemove = cart.getCartItems().stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ProductNotFoundException(String.format("Product with id %d not found in cart", productId)));

        cart.getCartItems().remove(itemToRemove);
        Cart updatedCart = this.cartRepository.save(cart);
        log.info("Product ID {} removed from cart ID {}", productId, cart.getId());

        return this.cartMapper.toDTO(updatedCart);
    }

    @Override
    @Transactional
    public void clearCart(final User user) {
        log.info("Clearing cart for user '{}'", user.getUserName());

        Cart cart = this.cartRepository.findByUser(user)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user"));

        cart.getCartItems().clear();
        this.cartRepository.save(cart);
        log.info("Cart cleared successfully for user '{}'", user.getUserName());
    }
}
