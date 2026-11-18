package com.ecommerce.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.ecommerce.model.Product;

@DataJpaTest
@DisplayName("ProductRepository JPA Slice Tests")
class ProductRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private Product laptop;
    private Product phone;

    @BeforeEach
    void setUp() {
        laptop = Product.builder()
                .name("MacBook Pro 16")
                .description("Apple M3 Max")
                .itemCode(1001)
                .manufacturer("Apple")
                .category("Laptop")
                .stock(15)
                .price(2499.99)
                .build();

        phone = Product.builder()
                .name("iPhone 15 Pro")
                .description("Titanium body")
                .itemCode(1002)
                .manufacturer("Apple")
                .category("Mobile")
                .stock(30)
                .price(999.99)
                .build();

        entityManager.persistAndFlush(laptop);
        entityManager.persistAndFlush(phone);
    }

    @Test
    @DisplayName("findByItemCode should return present Optional for existing item code")
    void findByItemCode_ExistingItemCode_ReturnsOptionalPresent() {
        Optional<Product> found = productRepository.findByItemCode(1001);

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("MacBook Pro 16");
        assertThat(found.get().getPrice()).isEqualTo(2499.99);
    }

    @Test
    @DisplayName("findByItemCode should return empty Optional for non-existing item code")
    void findByItemCode_NonExistingItemCode_ReturnsEmptyOptional() {
        Optional<Product> found = productRepository.findByItemCode(9999);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByItemCode should return true for existing item code")
    void existsByItemCode_ExistingItemCode_ReturnsTrue() {
        boolean exists = productRepository.existsByItemCode(1002);

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByItemCode should return false for non-existing item code")
    void existsByItemCode_NonExistingItemCode_ReturnsFalse() {
        boolean exists = productRepository.existsByItemCode(8888);

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByNameContaining should return list of products matching substring")
    void findByNameContaining_MatchesSubstring_ReturnsList() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Product> products = productRepository.findByNameContaining("Pro", pageable);

        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getName)
                .containsExactlyInAnyOrder("MacBook Pro 16", "iPhone 15 Pro");
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase should match case-insensitively with pagination")
    void findByNameContainingIgnoreCase_CaseInsensitive_ReturnsPagedProducts() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = productRepository.findByNameContainingIgnoreCase("mAcBoOk", pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getItemCode()).isEqualTo(1001);
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase should return empty page when no match")
    void findByNameContainingIgnoreCase_NoMatch_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = productRepository.findByNameContainingIgnoreCase("Samsung", pageable);

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("findAll with Pageable should return paginated slice correctly")
    void findAll_WithPageable_ReturnsPaginatedProducts() {
        Pageable pageable = PageRequest.of(0, 1);
        Page<Product> page = productRepository.findAll(pageable);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("persisting duplicate product name should violate unique constraint")
    void save_DuplicateName_ThrowsException() {
        Product duplicateName = Product.builder()
                .name("MacBook Pro 16")
                .description("Another description")
                .itemCode(2001)
                .manufacturer("Apple")
                .category("Laptop")
                .stock(5)
                .price(2100.0)
                .build();

        assertThatThrownBy(() -> entityManager.persistAndFlush(duplicateName))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("persisting duplicate product itemCode should violate unique constraint")
    void save_DuplicateItemCode_ThrowsException() {
        Product duplicateCode = Product.builder()
                .name("Unique Laptop Name")
                .description("Another description")
                .itemCode(1001)
                .manufacturer("Apple")
                .category("Laptop")
                .stock(5)
                .price(2100.0)
                .build();

        assertThatThrownBy(() -> entityManager.persistAndFlush(duplicateCode))
                .isInstanceOf(Exception.class);
    }
}
