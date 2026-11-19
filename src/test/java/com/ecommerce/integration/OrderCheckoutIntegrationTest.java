package com.ecommerce.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ecommerce.dto.request.CartRequestDto;
import com.ecommerce.dto.request.ProductRequestDto;
import com.ecommerce.dto.request.UpdateCartItemRequest;
import com.ecommerce.model.Cart;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;

@DisplayName("Commerce & Order Checkout Full Integration Tests (MySQL Testcontainers)")
class OrderCheckoutIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("The Ultimate E2E Commerce Journey: Add to cart -> Update quantity -> Checkout -> Deduct stock in MySQL -> Clear cart")
    void checkoutOrder_FullCommerceJourney_DeductsStockClearsCartAndCreatesOrder() {
        String adminToken = createAdminAndGetToken("admin_checkout_flow");
        HttpHeaders adminHeaders = createAuthHeaders(adminToken);
        ProductRequestDto newProduct = ProductRequestDto.builder()
                .name("Logitech MX Master 3S")
                .description("Performance Wireless Mouse")
                .itemCode(8001)
                .manufacturer("Logitech")
                .category("Accessories")
                .stock(10)
                .price(100.0)
                .build();
        ResponseEntity<JsonNode> productRes = restTemplate.postForEntity(
                "/api/products",
                new HttpEntity<>(newProduct, adminHeaders),
                JsonNode.class
        );
        Integer productId = productRes.getBody().get("data").get("id").asInt();

        String userToken = createUserAndGetToken("shopper_mx");
        HttpHeaders userHeaders = createAuthHeaders(userToken);

        CartRequestDto addRequest = CartRequestDto.builder()
                .productId(productId)
                .quantity(2)
                .build();
        ResponseEntity<JsonNode> addRes = restTemplate.postForEntity(
                "/api/cart/items",
                new HttpEntity<>(addRequest, userHeaders),
                JsonNode.class
        );
        assertThat(addRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        UpdateCartItemRequest updateRequest = UpdateCartItemRequest.builder()
                .quantity(3)
                .build();
        ResponseEntity<JsonNode> updateRes = restTemplate.exchange(
                "/api/cart/items/" + productId,
                HttpMethod.PUT,
                new HttpEntity<>(updateRequest, userHeaders),
                JsonNode.class
        );
        assertThat(updateRes.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<JsonNode> checkoutRes = restTemplate.postForEntity(
                "/api/orders/checkout",
                new HttpEntity<>(userHeaders),
                JsonNode.class
        );

        assertThat(checkoutRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(checkoutRes.getBody()).isNotNull();
        assertThat(checkoutRes.getBody().get("operationType").asText()).isEqualTo("SUCCESS");
        Integer orderId = checkoutRes.getBody().get("data").get("orderId").asInt();
        assertThat(orderId).isPositive();

        // Verify state changes persisted in MySQL container
        Optional<Product> updatedProduct = productRepository.findById(productId);
        assertThat(updatedProduct).isPresent();
        assertThat(updatedProduct.get().getStock()).isEqualTo(7);

        User shopper = userRepository.findByUserName("shopper_mx").orElseThrow();
        Optional<Cart> userCart = cartRepository.findByUser(shopper);
        assertThat(userCart).isPresent();
        assertThat(cartItemRepository.findByCart(userCart.get())).isEmpty();

        assertThat(orderRepository.findById(orderId)).isPresent();
    }

    @Test
    @DisplayName("Checkout validation: attempting checkout with empty cart fails with 400 Bad Request")
    void checkoutOrder_EmptyCart_ReturnsBadRequest() {
        String userToken = createUserAndGetToken("shopper_empty");
        HttpHeaders headers = createAuthHeaders(userToken);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/orders/checkout",
                new HttpEntity<>(headers),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("operationType").asText()).isEqualTo("FAILURE");
    }

    @Test
    @DisplayName("Anti-IDOR Security: Customer B cannot view Customer A's order details")
    void getOrderDetail_AntiIDOR_RejectsUnauthorizedUser() {
        String adminToken = createAdminAndGetToken("admin_idor_setup");
        HttpHeaders adminHeaders = createAuthHeaders(adminToken);
        ProductRequestDto product = ProductRequestDto.builder()
                .name("Secure Flash Drive")
                .itemCode(8002)
                .stock(5)
                .price(20.0)
                .build();
        ResponseEntity<JsonNode> pRes = restTemplate.postForEntity(
                "/api/products",
                new HttpEntity<>(product, adminHeaders),
                JsonNode.class
        );
        Integer prodId = pRes.getBody().get("data").get("id").asInt();

        String userAToken = createUserAndGetToken("customer_alice_idor");
        HttpHeaders userAHeaders = createAuthHeaders(userAToken);
        restTemplate.postForEntity(
                "/api/cart/items",
                new HttpEntity<>(CartRequestDto.builder().productId(prodId).quantity(1).build(), userAHeaders),
                JsonNode.class
        );
        ResponseEntity<JsonNode> checkoutRes = restTemplate.postForEntity(
                "/api/orders/checkout",
                new HttpEntity<>(userAHeaders),
                JsonNode.class
        );
        Integer orderId = checkoutRes.getBody().get("data").get("orderId").asInt();

        String userBToken = createUserAndGetToken("customer_bob_idor");
        HttpHeaders userBHeaders = createAuthHeaders(userBToken);

        ResponseEntity<JsonNode> unauthorizedAccess = restTemplate.exchange(
                "/api/orders/" + orderId,
                HttpMethod.GET,
                new HttpEntity<>(userBHeaders),
                JsonNode.class
        );

        assertThat(unauthorizedAccess.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(unauthorizedAccess.getBody().get("operationType").asText()).isEqualTo("FAILURE");
    }

    @Test
    @DisplayName("Role Access: Administrator is authorized to view any customer's order detail")
    void getOrderDetail_AdminAccess_AllowsAdminToViewAnyOrder() {
        String setupAdminToken = createAdminAndGetToken("admin_setup_view");
        HttpHeaders setupHeaders = createAuthHeaders(setupAdminToken);
        ProductRequestDto prod = ProductRequestDto.builder()
                .name("Gaming Monitor 144Hz")
                .itemCode(8003)
                .stock(5)
                .price(250.0)
                .build();
        ResponseEntity<JsonNode> pRes = restTemplate.postForEntity(
                "/api/products",
                new HttpEntity<>(prod, setupHeaders),
                JsonNode.class
        );
        Integer prodId = pRes.getBody().get("data").get("id").asInt();

        String userToken = createUserAndGetToken("user_order_view");
        HttpHeaders userHeaders = createAuthHeaders(userToken);
        restTemplate.postForEntity(
                "/api/cart/items",
                new HttpEntity<>(CartRequestDto.builder().productId(prodId).quantity(1).build(), userHeaders),
                JsonNode.class
        );
        ResponseEntity<JsonNode> checkoutRes = restTemplate.postForEntity(
                "/api/orders/checkout",
                new HttpEntity<>(userHeaders),
                JsonNode.class
        );
        Integer orderId = checkoutRes.getBody().get("data").get("orderId").asInt();

        String auditorAdminToken = createAdminAndGetToken("admin_auditor");
        HttpHeaders auditorHeaders = createAuthHeaders(auditorAdminToken);

        ResponseEntity<JsonNode> adminView = restTemplate.exchange(
                "/api/orders/" + orderId,
                HttpMethod.GET,
                new HttpEntity<>(auditorHeaders),
                JsonNode.class
        );

        assertThat(adminView.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminView.getBody()).isNotNull();
        assertThat(adminView.getBody().get("data").get("orderId").asInt()).isEqualTo(orderId);
    }
}
