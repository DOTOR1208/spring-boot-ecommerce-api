package com.ecommerce.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;

@DataJpaTest
@DisplayName("CartItemRepository JPA Slice Tests")
class CartItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartItemRepository cartItemRepository;

    private Cart cart;
    private Product product1;
    private Product product2;
    private CartItem item1;

    @BeforeEach
    void setUp() {
        User user = entityManager.persistAndFlush(
                User.builder()
                        .name("Charlie")
                        .userName("charlie")
                        .password("secret")
                        .email("charlie@example.com")
                        .deleted(false)
                        .build()
        );

        product1 = entityManager.persistAndFlush(
                Product.builder()
                        .name("Keyboard Mechanical")
                        .description("RGB Gaming Keyboard")
                        .itemCode(4001)
                        .manufacturer("Keychron")
                        .category("Hardware")
                        .stock(20)
                        .price(89.99)
                        .build()
        );

        product2 = entityManager.persistAndFlush(
                Product.builder()
                        .name("Gaming Mousepad")
                        .description("Extra Large Pad")
                        .itemCode(4002)
                        .manufacturer("SteelSeries")
                        .category("Accessories")
                        .stock(100)
                        .price(19.99)
                        .build()
        );

        cart = entityManager.persistAndFlush(
                Cart.builder()
                        .user(user)
                        .build()
        );

        item1 = entityManager.persistAndFlush(
                CartItem.builder()
                        .cart(cart)
                        .product(product1)
                        .quantity(1)
                        .build()
        );
    }

    @Test
    @DisplayName("findByCart should return all items belonging to the cart")
    void findByCart_ExistingItems_ReturnsList() {
        CartItem item2 = entityManager.persistAndFlush(
                CartItem.builder()
                        .cart(cart)
                        .product(product2)
                        .quantity(3)
                        .build()
        );

        List<CartItem> items = cartItemRepository.findByCart(cart);

        assertThat(items).hasSize(2);
        assertThat(items).extracting(CartItem::getQuantity).containsExactlyInAnyOrder(1, 3);
    }

    @Test
    @DisplayName("findByCart should return empty list when cart has no items")
    void findByCart_EmptyCart_ReturnsEmptyList() {
        User otherUser = entityManager.persistAndFlush(
                User.builder()
                        .name("Diana")
                        .userName("diana")
                        .password("secret")
                        .email("diana@example.com")
                        .deleted(false)
                        .build()
        );
        Cart emptyCart = entityManager.persistAndFlush(
                Cart.builder()
                        .user(otherUser)
                        .build()
        );

        List<CartItem> items = cartItemRepository.findByCart(emptyCart);

        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("findByCartAndProduct should return item when product is present in cart")
    void findByCartAndProduct_ExistingItem_ReturnsOptionalPresent() {
        Optional<CartItem> found = cartItemRepository.findByCartAndProduct(cart, product1);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(item1.getId());
        assertThat(found.get().getQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("findByCartAndProduct should return empty when product is not in cart")
    void findByCartAndProduct_NonExistingItem_ReturnsEmptyOptional() {
        Optional<CartItem> found = cartItemRepository.findByCartAndProduct(cart, product2);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("deleteByCart should remove all items associated with that cart")
    void deleteByCart_RemovesAllItemsInCart() {
        cartItemRepository.deleteByCart(cart);
        entityManager.flush();
        entityManager.clear();

        List<CartItem> remaining = cartItemRepository.findByCart(cart);
        assertThat(remaining).isEmpty();
    }
}
