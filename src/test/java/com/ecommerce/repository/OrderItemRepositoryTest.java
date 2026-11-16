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
@DisplayName("OrderItemRepository JPA Slice Tests")
class OrderItemRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Orders order;
    private Product product1;
    private Product product2;
    private OrderItem orderItem1;

    @BeforeEach
    void setUp() {
        User user = entityManager.persistAndFlush(
                User.builder()
                        .name("Grace")
                        .userName("grace")
                        .password("secret")
                        .email("grace@example.com")
                        .deleted(false)
                        .build()
        );

        product1 = entityManager.persistAndFlush(
                Product.builder()
                        .name("Monitor 27 inch 4K")
                        .description("IPS UHD Display")
                        .itemCode(6001)
                        .manufacturer("Dell")
                        .category("Monitor")
                        .stock(10)
                        .price(399.99)
                        .build()
        );

        product2 = entityManager.persistAndFlush(
                Product.builder()
                        .name("Webcam 1080p")
                        .description("Full HD Webcam")
                        .itemCode(6002)
                        .manufacturer("Logitech")
                        .category("Accessories")
                        .stock(25)
                        .price(59.99)
                        .build()
        );

        order = entityManager.persistAndFlush(
                Orders.builder()
                        .user(user)
                        .orderDate(LocalDateTime.now())
                        .status(OrderStatus.COMPLETED)
                        .build()
        );

        orderItem1 = entityManager.persistAndFlush(
                OrderItem.builder()
                        .order(order)
                        .product(product1)
                        .quantity(1)
                        .priceAtOrderTime(399.99)
                        .build()
        );
    }

    @Test
    @DisplayName("findByOrder should return all items belonging to the order")
    void findByOrder_ExistingItems_ReturnsList() {
        OrderItem orderItem2 = entityManager.persistAndFlush(
                OrderItem.builder()
                        .order(order)
                        .product(product2)
                        .quantity(2)
                        .priceAtOrderTime(59.99)
                        .build()
        );

        List<OrderItem> items = orderItemRepository.findByOrder(order);

        assertThat(items).hasSize(2);
        assertThat(items).extracting(OrderItem::getQuantity).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    @DisplayName("findByOrder should return empty list when order has no items")
    void findByOrder_OrderWithoutItems_ReturnsEmptyList() {
        User otherUser = entityManager.persistAndFlush(
                User.builder()
                        .name("Hank")
                        .userName("hank")
                        .password("secret")
                        .email("hank@example.com")
                        .deleted(false)
                        .build()
        );
        Orders emptyOrder = entityManager.persistAndFlush(
                Orders.builder()
                        .user(otherUser)
                        .orderDate(LocalDateTime.now())
                        .status(OrderStatus.PENDING)
                        .build()
        );

        List<OrderItem> items = orderItemRepository.findByOrder(emptyOrder);

        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("findByOrderId should return items for valid orderId")
    void findByOrderId_ExistingOrderId_ReturnsList() {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getId()).isEqualTo(orderItem1.getId());
        assertThat(items.get(0).getPriceAtOrderTime()).isEqualTo(399.99);
    }

    @Test
    @DisplayName("findByOrderId should return empty list for non-existing orderId")
    void findByOrderId_NonExistingOrderId_ReturnsEmptyList() {
        List<OrderItem> items = orderItemRepository.findByOrderId(9999);

        assertThat(items).isEmpty();
    }
}
