package com.ecommerce.repository;

import static org.assertj.core.api.Assertions.assertThat;

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
@DisplayName("CartRepository JPA Slice Tests")
class CartRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CartRepository cartRepository;

    private User sampleUser;
    private Cart sampleCart;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleUser = entityManager.persistAndFlush(
                User.builder()
                        .name("Alice")
                        .userName("alice")
                        .password("secret")
                        .email("alice@example.com")
                        .deleted(false)
                        .build()
        );

        sampleProduct = entityManager.persistAndFlush(
                Product.builder()
                        .name("Wireless Mouse")
                        .description("Ergonomic Bluetooth Mouse")
                        .itemCode(3001)
                        .manufacturer("Logitech")
                        .category("Accessories")
                        .stock(50)
                        .price(29.99)
                        .build()
        );

        sampleCart = Cart.builder()
                .user(sampleUser)
                .build();
        sampleCart = entityManager.persistAndFlush(sampleCart);
    }

    @Test
    @DisplayName("findByUser should return cart when user has an active cart")
    void findByUser_ExistingCart_ReturnsOptionalPresent() {
        Optional<Cart> found = cartRepository.findByUser(sampleUser);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(sampleCart.getId());
        assertThat(found.get().getUser().getUserName()).isEqualTo("alice");
    }

    @Test
    @DisplayName("findByUser should return empty when user has no cart")
    void findByUser_UserWithoutCart_ReturnsEmptyOptional() {
        User otherUser = entityManager.persistAndFlush(
                User.builder()
                        .name("Bob")
                        .userName("bob")
                        .password("secret")
                        .email("bob@example.com")
                        .deleted(false)
                        .build()
        );

        Optional<Cart> found = cartRepository.findByUser(otherUser);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByUserId should return cart for given valid userId")
    void findByUserId_ExistingUserId_ReturnsOptionalPresent() {
        Optional<Cart> found = cartRepository.findByUserId(sampleUser.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(sampleCart.getId());
    }

    @Test
    @DisplayName("findByUserId should return empty for non-existing userId")
    void findByUserId_NonExistingUserId_ReturnsEmptyOptional() {
        Optional<Cart> found = cartRepository.findByUserId(9999);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("save cart with cart items should cascade persist cart items")
    void save_CartWithItems_CascadePersistsItems() {
        CartItem item = CartItem.builder()
                .cart(sampleCart)
                .product(sampleProduct)
                .quantity(2)
                .build();
        sampleCart.getCartItems().add(item);

        Cart updated = cartRepository.save(sampleCart);
        entityManager.flush();
        entityManager.clear();

        Cart reloaded = entityManager.find(Cart.class, updated.getId());
        assertThat(reloaded.getCartItems()).hasSize(1);
        assertThat(reloaded.getCartItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(reloaded.getCartItems().get(0).getProduct().getName()).isEqualTo("Wireless Mouse");
    }

    @Test
    @DisplayName("delete cart should cascade and remove orphan cart items")
    void delete_Cart_CascadeRemovesOrphanItems() {
        CartItem item = CartItem.builder()
                .cart(sampleCart)
                .product(sampleProduct)
                .quantity(1)
                .build();
        sampleCart.getCartItems().add(item);
        entityManager.persistAndFlush(sampleCart);

        Integer cartId = sampleCart.getId();
        Integer itemId = sampleCart.getCartItems().get(0).getId();

        cartRepository.delete(sampleCart);
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Cart.class, cartId)).isNull();
        assertThat(entityManager.find(CartItem.class, itemId)).isNull();
    }
}
