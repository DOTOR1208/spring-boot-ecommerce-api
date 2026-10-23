package com.ecommerce.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.dto.response.OrderDetailResponseDto;
import com.ecommerce.dto.response.OrderResponseDto;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.CartNotFoundException;
import com.ecommerce.exception.OrderNotFoundException;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.Orders;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.model.enums.OrderStatus;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
    public OrderResponseDto checkoutOrder(final User user) {
        log.info("Processing checkout for user: '{}'", user.getUserName());

        Cart cart = this.cartRepository.findByUser(user)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for user"));

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            log.warn("Checkout rejected: cart is empty for user '{}'", user.getUserName());
            throw new BadRequestException("Cart is empty. Please add products to cart before checkout.");
        }

        for (CartItem item : cart.getCartItems()) {
            Product product = item.getProduct();
            if (product == null) {
                throw new BadRequestException("Cart contains an invalid product item");
            }

            if (product.getStock() < item.getQuantity()) {
                log.warn("Checkout failed: insufficient stock for product '{}'. Available: {}, Requested: {}",
                        product.getName(), product.getStock(), item.getQuantity());
                throw new BadRequestException(String.format("Insufficient stock for product '%s'. Available: %d, Requested: %d",
                        product.getName(), product.getStock(), item.getQuantity()));
            }

            product.setStock(product.getStock() - item.getQuantity());
            this.productRepository.save(product);
        }

        Orders order = Orders.builder()
                .user(user)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.COMPLETED)
                .build();

        List<OrderItem> orderItems = cart.getCartItems().stream().map(cartItem ->
                OrderItem.builder()
                        .order(order)
                        .product(cartItem.getProduct())
                        .quantity(cartItem.getQuantity())
                        .priceAtOrderTime(cartItem.getProduct().getPrice())
                        .build()
        ).toList();

        order.setOrderItems(orderItems);
        Orders savedOrder = this.orderRepository.save(order);

        cart.getCartItems().clear();
        this.cartRepository.save(cart);

        log.info("Checkout completed successfully for user '{}'. Order ID: {}", user.getUserName(), savedOrder.getId());
        return this.orderMapper.toSummaryDTO(savedOrder);
    }

    @Override
    public List<OrderResponseDto> getOrderSummaries(final User user) {
        log.info("Fetching order summaries for user: '{}'", user.getUserName());
        List<Orders> orders = this.orderRepository.findByUserOrderByOrderDateDesc(user);
        return this.orderMapper.toSummaryDTOs(orders);
    }

    @Override
    public OrderDetailResponseDto getOrderDetail(final Integer orderId, final User user) {
        log.info("Fetching order detail for order ID {} and user '{}'", orderId, user.getUserName());

        Orders order = this.orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(String.format("Order not found with id: %d", orderId)));

        boolean isOwner = order.getUser() != null && order.getUser().getId().equals(user.getId());
        boolean isAdmin = user.getRoles() != null && user.getRoles().stream()
                .anyMatch(r -> "ADMIN".equalsIgnoreCase(r.getName()) || "ROLE_ADMIN".equalsIgnoreCase(r.getName()));

        if (!isOwner && !isAdmin) {
            log.warn("Access denied to order ID {} for user '{}'", orderId, user.getUserName());
            throw new BadRequestException("You do not have permission to view this order");
        }

        return this.orderMapper.toDetailDTO(order);
    }
}
