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

import java.util.ArrayList;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.ecommerce.service.impl.CartServiceImpl;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartServiceImpl cartService;

    private User sampleUser;
    private Cart sampleCart;
    private Product sampleProduct;
    private CartResponseDto sampleCartResponseDto;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1)
                .userName("john_doe")
                .email("john@example.com")
                .build();

        sampleCart = Cart.builder()
                .id(10)
                .user(sampleUser)
                .cartItems(new ArrayList<>())
                .build();

        sampleProduct = Product.builder()
                .id(100)
                .name("Wireless Mouse")
                .price(25.0)
                .stock(5)
                .build();

        sampleCartResponseDto = CartResponseDto.builder()
                .cartItems(new ArrayList<>())
                .totalPrice(0.0)
                .build();
    }

    // ==========================================
    // 1. Tests for getCart
    // ==========================================

    @Test
    @DisplayName("getCart: Should return CartResponseDto when cart exists for user")
    void getCart_WhenCartExists_ShouldReturnCartDto() {
        // Arrange
        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.of(sampleCart));
        when(cartMapper.toDTO(sampleCart)).thenReturn(sampleCartResponseDto);

        // Act
        CartResponseDto result = cartService.getCart(sampleUser);

        // Assert
        assertNotNull(result);
        verify(cartRepository, times(1)).findByUser(sampleUser);
        verify(cartMapper, times(1)).toDTO(sampleCart);
    }

    @Test
    @DisplayName("getCart: Should create and return new cart when user has no cart")
    void getCart_WhenCartDoesNotExist_ShouldCreateAndReturnNewCart() {
        // Arrange
        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);
        when(cartMapper.toDTO(sampleCart)).thenReturn(sampleCartResponseDto);

        // Act
        CartResponseDto result = cartService.getCart(sampleUser);

        // Assert
        assertNotNull(result);
        verify(cartRepository, times(1)).findByUser(sampleUser);
        verify(cartRepository, times(1)).save(any(Cart.class));
        verify(cartMapper, times(1)).toDTO(sampleCart);
    }

    // ==========================================
    // 2. Tests for addToCart
    // ==========================================

    @Test
    @DisplayName("addToCart: Should throw ProductNotFoundException when product does not exist")
    void addToCart_WhenProductNotFound_ShouldThrowProductNotFoundException() {
        // Arrange
        CartRequestDto request = CartRequestDto.builder()
                .productId(999)
                .quantity(1)
                .build();

        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.of(sampleCart));
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> {
            cartService.addToCart(sampleUser, request);
        });
        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    @Test
    @DisplayName("addToCart: Should add new item when product does not exist in cart")
    void addToCart_WhenProductNotInCart_ShouldAddNewItem() {
        // Arrange
        CartRequestDto request = CartRequestDto.builder()
                .productId(100)
                .quantity(2)
                .build();

        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.of(sampleCart));
        when(productRepository.findById(100)).thenReturn(Optional.of(sampleProduct));
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);
        when(cartMapper.toDTO(sampleCart)).thenReturn(sampleCartResponseDto);

        // Act
        CartResponseDto result = cartService.addToCart(sampleUser, request);

        // Assert
        assertNotNull(result);
        assertEquals(1, sampleCart.getCartItems().size());
        assertEquals(2, sampleCart.getCartItems().get(0).getQuantity());
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
        verify(cartRepository, times(1)).save(sampleCart);
    }

    @Test
    @DisplayName("addToCart: Should increment quantity when product already exists in cart")
    void addToCart_WhenProductAlreadyInCart_ShouldIncrementQuantity() {
        // Arrange
        CartItem existingItem = CartItem.builder()
                .id(1)
                .cart(sampleCart)
                .product(sampleProduct)
                .quantity(2)
                .build();
        sampleCart.getCartItems().add(existingItem);

        CartRequestDto request = CartRequestDto.builder()
                .productId(100)
                .quantity(1)
                .build();

        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.of(sampleCart));
        when(productRepository.findById(100)).thenReturn(Optional.of(sampleProduct));
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);
        when(cartMapper.toDTO(sampleCart)).thenReturn(sampleCartResponseDto);

        // Act
        CartResponseDto result = cartService.addToCart(sampleUser, request);

        // Assert
        assertNotNull(result);
        assertEquals(1, sampleCart.getCartItems().size());
        assertEquals(3, existingItem.getQuantity()); // 2 + 1 = 3
        verify(cartItemRepository, times(1)).save(existingItem);
    }

    @Test
    @DisplayName("addToCart: Should throw BadRequestException when total quantity exceeds available stock")
    void addToCart_WhenQuantityExceedsStock_ShouldThrowBadRequestException() {
        // Arrange: Product stock is 5, requesting 6
        CartRequestDto request = CartRequestDto.builder()
                .productId(100)
                .quantity(6)
                .build();

        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.of(sampleCart));
        when(productRepository.findById(100)).thenReturn(Optional.of(sampleProduct));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            cartService.addToCart(sampleUser, request);
        });

        verify(cartItemRepository, never()).save(any(CartItem.class));
    }

    // ==========================================
    // 3. Tests for updateCartItemQuantity
    // ==========================================

    @Test
    @DisplayName("updateCartItemQuantity: Should throw ProductNotFoundException when product is not in cart")
    void updateCartItemQuantity_WhenProductNotFoundInCart_ShouldThrowProductNotFoundException() {
        // Arrange: Cart is empty
        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.of(sampleCart));

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> {
            cartService.updateCartItemQuantity(sampleUser, 999, 2);
        });
    }

    @Test
    @DisplayName("updateCartItemQuantity: Should update quantity when quantity is positive and in stock")
    void updateCartItemQuantity_WhenValidQuantity_ShouldUpdateQuantity() {
        // Arrange
        CartItem existingItem = CartItem.builder()
                .id(1)
                .cart(sampleCart)
                .product(sampleProduct)
                .quantity(2)
                .build();
        sampleCart.getCartItems().add(existingItem);

        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);
        when(cartMapper.toDTO(sampleCart)).thenReturn(sampleCartResponseDto);

        // Act: Update quantity to 4 (within stock of 5)
        CartResponseDto result = cartService.updateCartItemQuantity(sampleUser, 100, 4);

        // Assert
        assertNotNull(result);
        assertEquals(4, existingItem.getQuantity());
        verify(cartItemRepository, times(1)).save(existingItem);
    }

    @Test
    @DisplayName("updateCartItemQuantity: Should remove item from cart when quantity is zero or negative")
    void updateCartItemQuantity_WhenQuantityIsZeroOrNegative_ShouldRemoveItem() {
        // Arrange
        CartItem existingItem = CartItem.builder()
                .id(1)
                .cart(sampleCart)
                .product(sampleProduct)
                .quantity(2)
                .build();
        sampleCart.getCartItems().add(existingItem);

        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(sampleCart);
        when(cartMapper.toDTO(sampleCart)).thenReturn(sampleCartResponseDto);

        // Act: Update quantity to 0
        CartResponseDto result = cartService.updateCartItemQuantity(sampleUser, 100, 0);

        // Assert
        assertNotNull(result);
        assertTrue(sampleCart.getCartItems().isEmpty()); // Automatically removed
        verify(cartItemRepository, never()).save(existingItem);
    }

    @Test
    @DisplayName("updateCartItemQuantity: Should throw BadRequestException when quantity exceeds available stock")
    void updateCartItemQuantity_WhenQuantityExceedsStock_ShouldThrowBadRequestException() {
        // Arrange
        CartItem existingItem = CartItem.builder()
                .id(1)
                .cart(sampleCart)
                .product(sampleProduct)
                .quantity(2)
                .build();
        sampleCart.getCartItems().add(existingItem);

        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.of(sampleCart));

        // Act & Assert: Stock is 5, requested 10
        assertThrows(BadRequestException.class, () -> {
            cartService.updateCartItemQuantity(sampleUser, 100, 10);
        });

        assertEquals(2, existingItem.getQuantity()); // Unchanged
        verify(cartItemRepository, never()).save(existingItem);
    }

    // ==========================================
    // 4. Tests for removeProductFromCart & clearCart
    // ==========================================

    @Test
    @DisplayName("removeProductFromCart: Should throw ProductNotFoundException when product is not in cart")
    void removeProductFromCart_WhenProductNotInCart_ShouldThrowProductNotFoundException() {
        // Arrange
        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.of(sampleCart));

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> {
            cartService.removeProductFromCart(sampleUser, 999);
        });
    }

    @Test
    @DisplayName("removeProductFromCart: Should remove item when product exists in cart")
    void removeProductFromCart_WhenProductExists_ShouldRemoveItem() {
        // Arrange
        CartItem item = CartItem.builder()
                .id(1)
                .cart(sampleCart)
                .product(sampleProduct)
                .quantity(2)
                .build();
        sampleCart.getCartItems().add(item);

        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(sampleCart)).thenReturn(sampleCart);
        when(cartMapper.toDTO(sampleCart)).thenReturn(sampleCartResponseDto);

        // Act
        CartResponseDto result = cartService.removeProductFromCart(sampleUser, 100);

        // Assert
        assertNotNull(result);
        assertTrue(sampleCart.getCartItems().isEmpty());
        verify(cartRepository, times(1)).save(sampleCart);
    }

    @Test
    @DisplayName("clearCart: Should remove all items from cart")
    void clearCart_ShouldEmptyAllItems() {
        // Arrange
        CartItem item = CartItem.builder()
                .id(1)
                .cart(sampleCart)
                .product(sampleProduct)
                .quantity(2)
                .build();
        sampleCart.getCartItems().add(item);

        when(cartRepository.findByUser(sampleUser)).thenReturn(Optional.of(sampleCart));
        when(cartRepository.save(sampleCart)).thenReturn(sampleCart);

        // Act
        cartService.clearCart(sampleUser);

        // Assert
        assertTrue(sampleCart.getCartItems().isEmpty());
        verify(cartRepository, times(1)).save(sampleCart);
    }
}
