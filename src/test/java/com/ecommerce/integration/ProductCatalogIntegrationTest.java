package com.ecommerce.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ecommerce.dto.request.ProductRequestDto;
import com.ecommerce.model.Product;
import com.fasterxml.jackson.databind.JsonNode;

@DisplayName("Product Catalog Full Integration Tests (MySQL Testcontainers)")
class ProductCatalogIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Admin create product: persists in real MySQL and returns 200 OK")
    void adminCreateProduct_ValidPayload_PersistsInMySQLAndReturns200() {
        String adminToken = createAdminAndGetToken("admin_catalog_1");
        HttpHeaders headers = createAuthHeaders(adminToken);

        ProductRequestDto request = ProductRequestDto.builder()
                .name("MacBook Pro 14 M4")
                .description("Latest Apple Silicon")
                .itemCode(7001)
                .manufacturer("Apple")
                .category("Laptop")
                .stock(15)
                .price(1999.99)
                .build();

        HttpEntity<ProductRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/products",
                entity,
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("operationType").asText()).isEqualTo("SUCCESS");
        assertThat(response.getBody().get("data").get("name").asText()).isEqualTo("MacBook Pro 14 M4");

        // Verify entity persisted in real MySQL container
        Optional<Product> persisted = productRepository.findByItemCode(7001);
        assertThat(persisted).isPresent();
        assertThat(persisted.get().getStock()).isEqualTo(15);
        assertThat(persisted.get().getPrice()).isEqualTo(1999.99);
    }

    @Test
    @DisplayName("RBAC check: Regular user attempting to create product gets 403 Forbidden")
    void userCreateProduct_Forbidden_Returns403() {
        String userToken = createUserAndGetToken("regular_user_catalog");
        HttpHeaders headers = createAuthHeaders(userToken);

        ProductRequestDto request = ProductRequestDto.builder()
                .name("Hacker Product")
                .itemCode(7002)
                .stock(10)
                .price(99.0)
                .build();

        HttpEntity<ProductRequestDto> entity = new HttpEntity<>(request, headers);

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/products",
                entity,
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Public browsing: client without token can view product catalog")
    void publicBrowseProducts_WithoutToken_ReturnsProductList() {
        String adminToken = createAdminAndGetToken("admin_catalog_browse");
        HttpHeaders headers = createAuthHeaders(adminToken);
        ProductRequestDto request = ProductRequestDto.builder()
                .name("Dell UltraSharp 32")
                .itemCode(7003)
                .stock(8)
                .price(899.0)
                .build();
        restTemplate.postForEntity("/api/products", new HttpEntity<>(request, headers), JsonNode.class);

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/products",
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("operationType").asText()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("Public search: search products by name returns matching items")
    void publicSearchProducts_MatchingName_ReturnsFilteredPage() {
        String adminToken = createAdminAndGetToken("admin_catalog_search");
        HttpHeaders headers = createAuthHeaders(adminToken);
        ProductRequestDto request = ProductRequestDto.builder()
                .name("Sony WH-1000XM5 Headphones")
                .itemCode(7004)
                .stock(20)
                .price(399.0)
                .build();
        restTemplate.postForEntity("/api/products", new HttpEntity<>(request, headers), JsonNode.class);

        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                "/api/products/search?name=Sony",
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("data").toString()).contains("Sony WH-1000XM5");
    }

    @Test
    @DisplayName("Duplicate itemCode: Admin attempting duplicate itemCode gets 400 Bad Request")
    void adminCreateProduct_DuplicateItemCode_ReturnsBadRequest() {
        String adminToken = createAdminAndGetToken("admin_catalog_dup");
        HttpHeaders headers = createAuthHeaders(adminToken);

        ProductRequestDto product1 = ProductRequestDto.builder()
                .name("Product Original")
                .itemCode(7005)
                .stock(5)
                .price(50.0)
                .build();
        restTemplate.postForEntity("/api/products", new HttpEntity<>(product1, headers), JsonNode.class);

        ProductRequestDto duplicate = ProductRequestDto.builder()
                .name("Product Duplicate Code")
                .itemCode(7005)
                .stock(10)
                .price(60.0)
                .build();

        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                "/api/products",
                new HttpEntity<>(duplicate, headers),
                JsonNode.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("operationType").asText()).isEqualTo("FAILURE");
    }
}
