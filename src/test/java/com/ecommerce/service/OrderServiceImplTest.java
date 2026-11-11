package com.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.dto.response.OrderDetailResponseDto;
import com.ecommerce.dto.response.OrderResponseDto;
import com.ecommerce.exception.BadRequestException;
import com.ecommerce.exception.CartNotFoundException;
import com.ecommerce.exception.OrderNotFoundException;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Orders;
import com.ecommerce.model.Product;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;
import com.ecommerce.model.enums.OrderStatus;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.impl.OrderServiceImpl;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User sampleUser;
    private Cart sampleCart;
    private Product sampleProduct;
    private Orders sampleOrder;
    private OrderResponseDto sampleOrderSummaryDto;
    private OrderDetailResponseDto sampleOrderDetailDto;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1)
                .userName("john_doe")
                .email("john@example.com")
                .roles(new ArrayList<>())
                .build();

        sampleProduct = Product.builder()
                .id(100)
                .name("Mechanical Keyboard")
                .price(120.0)
                .stock(10)
                .build();

        sampleCart = Cart.builder()
                .id(5)
                .user(sampleUser)
                .cartItems(new ArrayList<>())
                .build();

        sampleOrder = Orders.builder()
                .id(50)
                .user(sampleUser)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.COMPLETED)
                .orderItems(new ArrayList<>())
                .build();

        sampleOrderSummaryDto = OrderResponseDto.builder()
                .orderId(50)
                .orderDate(LocalDateTime.now())
                .status("COMPLETED")
                .totalPrice(240.0)
                .build();

        sampleOrderDetailDto = OrderDetailResponseDto.builder()
                .orderId(50)
                .orderDate(LocalDateTime.now())
                .status("COMPLETED")
                .items(new ArrayList<>())
                .totalPrice(240.0)
                .build();
    }

    // ==========================================
    // 1. Tests for checkoutOrder
    // ==========================================

    @Test
    @DisplayName("checkoutOrder: Should deduct stock, snapshot price, clear cart and return OrderResponseDto")
    void checkoutOrder_WhenSuccessful_ShouldDeductStockSnapshotPriceAndClearCart() {
        // Arrange: Cart with 2 keyboards (stock is 10, price is 120.0)
        CartItem cartItem = CartItem.builder()
                .id(1)
                .cart(sampleCart)
                .product(sampleProduct)
                .quantity(2)
                .build();
        sampleCart.getCartItems().add(cartItem);

        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.of(sampleCart));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);
        when(orderRepository.save(any(Orders.class))).thenReturn(sampleOrder);
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);
        when(orderMapper.toSummaryDTO(sampleOrder)).thenReturn(sampleOrderSummaryDto);

        // Act
        OrderResponseDto result = orderService.checkoutOrder(sampleUser);

        // Assert
        assertNotNull(result);
        assertEquals(50, result.getOrderId());
        assertEquals("COMPLETED", result.getStatus());

        // Verify stock deducted: 10 - 2 = 8
        assertEquals(8, sampleProduct.getStock());
        verify(productRepository, times(1)).save(sampleProduct);

        // Verify order saved
        verify(orderRepository, times(1)).save(any(Orders.class));

        // Verify cart is cleared
        assertTrue(sampleCart.getCartItems().isEmpty());
        verify(cartRepository, times(1)).save(sampleCart);
    }

    @Test
    @DisplayName("checkoutOrder: Should throw CartNotFoundException when user has no cart")
    void checkoutOrder_WhenCartNotFound_ShouldThrowCartNotFoundException() {
        // Arrange
        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CartNotFoundException.class, () -> {
            orderService.checkoutOrder(sampleUser);
        });
        verify(orderRepository, never()).save(any(Orders.class));
    }

    @Test
    @DisplayName("checkoutOrder: Should throw BadRequestException when cart is empty")
    void checkoutOrder_WhenCartIsEmpty_ShouldThrowBadRequestException() {
        // Arrange: Cart with no items
        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.of(sampleCart));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            orderService.checkoutOrder(sampleUser);
        });
        verify(orderRepository, never()).save(any(Orders.class));
    }

    @Test
    @DisplayName("checkoutOrder: Should throw BadRequestException when product stock is insufficient")
    void checkoutOrder_WhenStockInsufficientAtCheckout_ShouldThrowBadRequestException() {
        // Arrange: Product stock is 1, but cart item requested 5
        sampleProduct.setStock(1);
        CartItem cartItem = CartItem.builder()
                .id(1)
                .cart(sampleCart)
                .product(sampleProduct)
                .quantity(5)
                .build();
        sampleCart.getCartItems().add(cartItem);

        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.of(sampleCart));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            orderService.checkoutOrder(sampleUser);
        });

        // Stock should remain untouched and no order created
        assertEquals(1, sampleProduct.getStock());
        verify(orderRepository, never()).save(any(Orders.class));
    }

    // ==========================================
    // 2. Tests for getOrderSummaries
    // ==========================================

    @Test
    @DisplayName("getOrderSummaries: Should return list of user order summaries")
    void getOrderSummaries_ShouldReturnListOfUserOrders() {
        // Arrange
        List<Orders> ordersList = List.of(sampleOrder);
        List<OrderResponseDto> dtoList = List.of(sampleOrderSummaryDto);

        when(orderRepository.findByUserOrderByOrderDateDesc(sampleUser)).thenReturn(ordersList);
        when(orderMapper.toSummaryDTOs(ordersList)).thenReturn(dtoList);

        // Act
        List<OrderResponseDto> result = orderService.getOrderSummaries(sampleUser);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(50, result.get(0).getOrderId());
        verify(orderRepository, times(1)).findByUserOrderByOrderDateDesc(sampleUser);
    }

    // ==========================================
    // 3. Tests for getOrderDetail (Anti-IDOR & Role Access)
    // ==========================================

    @Test
    @DisplayName("getOrderDetail: Should return detail when requesting user is order owner")
    void getOrderDetail_WhenUserIsOwner_ShouldReturnDetailDto() {
        // Arrange
        when(orderRepository.findById(50)).thenReturn(Optional.of(sampleOrder));
        when(orderMapper.toDetailDTO(sampleOrder)).thenReturn(sampleOrderDetailDto);

        // Act: sampleUser is the owner (ID 1 == ID 1)
        OrderDetailResponseDto result = orderService.getOrderDetail(50, sampleUser);

        // Assert
        assertNotNull(result);
        assertEquals(50, result.getOrderId());
        verify(orderRepository, times(1)).findById(50);
    }

    @Test
    @DisplayName("getOrderDetail: Should return detail when requesting user is Admin")
    void getOrderDetail_WhenUserIsAdmin_ShouldReturnDetailDto() {
        // Arrange: Order belongs to user ID 2
        User anotherUser = User.builder().id(2).userName("jane_doe").build();
        Orders anotherUserOrder = Orders.builder()
                .id(50)
                .user(anotherUser)
                .build();

        // Calling user has ROLE_ADMIN
        Role adminRole = Role.builder().name("ROLE_ADMIN").build();
        sampleUser.getRoles().add(adminRole);

        when(orderRepository.findById(50)).thenReturn(Optional.of(anotherUserOrder));
        when(orderMapper.toDetailDTO(anotherUserOrder)).thenReturn(sampleOrderDetailDto);

        // Act
        OrderDetailResponseDto result = orderService.getOrderDetail(50, sampleUser);

        // Assert
        assertNotNull(result);
        verify(orderRepository, times(1)).findById(50);
    }

    @Test
    @DisplayName("getOrderDetail: Should throw BadRequestException when user is neither owner nor admin (Anti-IDOR)")
    void getOrderDetail_WhenUserIsNotOwnerAndNotAdmin_ShouldThrowBadRequestException() {
        // Arrange: Order belongs to user ID 2
        User anotherUser = User.builder().id(2).userName("jane_doe").build();
        Orders anotherUserOrder = Orders.builder()
                .id(50)
                .user(anotherUser)
                .build();

        // Calling user is a normal user (ID 1, not owner, no admin role)
        when(orderRepository.findById(50)).thenReturn(Optional.of(anotherUserOrder));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            orderService.getOrderDetail(50, sampleUser);
        });
    }

    @Test
    @DisplayName("getOrderDetail: Should throw OrderNotFoundException when order does not exist")
    void getOrderDetail_WhenOrderNotFound_ShouldThrowOrderNotFoundException() {
        // Arrange
        when(orderRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class, () -> {
            orderService.getOrderDetail(999, sampleUser);
        });
    }
}
