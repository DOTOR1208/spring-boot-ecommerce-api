package com.ecommerce.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.ecommerce.model.OrderItem;
import com.ecommerce.model.Orders;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.model.enums.OrderStatus;

@DataJpaTest
@DisplayName("OrderRepository JPA Slice Tests")
class OrderRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    private User sampleUser;
    private Product sampleProduct;
    private Orders order1;
    private Orders order2;

    @BeforeEach
    void setUp() {
        sampleUser = entityManager.persistAndFlush(
                User.builder()
                        .name("Eve")
                        .userName("eve")
                        .password("secret")
                        .email("eve@example.com")
                        .deleted(false)
                        .build()
        );

        sampleProduct = entityManager.persistAndFlush(
                Product.builder()
                        .name("USB-C Hub")
                        .description("Multiport Adapter")
                        .itemCode(5001)
                        .manufacturer("Anker")
                        .category("Accessories")
                        .stock(40)
                        .price(49.99)
                        .build()
        );

        LocalDateTime now = LocalDateTime.now();

        order1 = entityManager.persistAndFlush(
                Orders.builder()
                        .user(sampleUser)
                        .orderDate(now.minusDays(2))
                        .status(OrderStatus.COMPLETED)
                        .build()
        );

        order2 = entityManager.persistAndFlush(
                Orders.builder()
                        .user(sampleUser)
                        .orderDate(now)
                        .status(OrderStatus.PENDING)
                        .build()
        );
    }

    @Test
    @DisplayName("findByUser should return all orders placed by user")
    void findByUser_ExistingOrders_ReturnsList() {
        List<Orders> orders = orderRepository.findByUser(sampleUser);

        assertThat(orders).hasSize(2);
    }

    @Test
    @DisplayName("findByUser should return empty list when user has no orders")
    void findByUser_UserWithoutOrders_ReturnsEmptyList() {
        User otherUser = entityManager.persistAndFlush(
                User.builder()
                        .name("Frank")
                        .userName("frank")
                        .password("secret")
                        .email("frank@example.com")
                        .deleted(false)
                        .build()
        );

        List<Orders> orders = orderRepository.findByUser(otherUser);

        assertThat(orders).isEmpty();
    }

    @Test
    @DisplayName("findByUserOrderByOrderDateDesc should return orders sorted newest first")
    void findByUserOrderByOrderDateDesc_MultipleOrders_ReturnsDescendingOrder() {
        List<Orders> orders = orderRepository.findByUserOrderByOrderDateDesc(sampleUser);

        assertThat(orders).hasSize(2);
        // Verify descending chronological order: order2 is newer than order1
        assertThat(orders.get(0).getId()).isEqualTo(order2.getId());
        assertThat(orders.get(1).getId()).isEqualTo(order1.getId());
    }

    @Test
    @DisplayName("findByUserId should return orders for given valid userId")
    void findByUserId_ExistingUserId_ReturnsList() {
        List<Orders> orders = orderRepository.findByUserId(sampleUser.getId());

        assertThat(orders).hasSize(2);
    }

    @Test
    @DisplayName("findByUserId should return empty list for non-existing userId")
    void findByUserId_NonExistingUserId_ReturnsEmptyList() {
        List<Orders> orders = orderRepository.findByUserId(9999);

        assertThat(orders).isEmpty();
    }

    @Test
    @DisplayName("save order with order items should cascade persist order items")
    void save_OrderWithItems_CascadePersistsOrderItems() {
        Orders newOrder = Orders.builder()
                .user(sampleUser)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .build();

        OrderItem item = OrderItem.builder()
                .order(newOrder)
                .product(sampleProduct)
                .quantity(3)
                .priceAtOrderTime(49.99)
                .build();
        newOrder.getOrderItems().add(item);

        Orders saved = orderRepository.save(newOrder);
        entityManager.flush();
        entityManager.clear();

        Orders reloaded = entityManager.find(Orders.class, saved.getId());
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getOrderItems()).hasSize(1);
        assertThat(reloaded.getOrderItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(reloaded.getOrderItems().get(0).getPriceAtOrderTime()).isEqualTo(49.99);
    }
}
