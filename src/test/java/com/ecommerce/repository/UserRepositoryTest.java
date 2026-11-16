package com.ecommerce.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

import com.ecommerce.model.User;

@DataJpaTest
@DisplayName("UserRepository JPA Slice Tests")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .name("John Doe")
                .userName("johndoe")
                .password("encoded_secret_pass")
                .email("john.doe@example.com")
                .deleted(false)
                .build();
        entityManager.persistAndFlush(sampleUser);
    }

    @Test
    @DisplayName("findByUserName should return present Optional when user exists")
    void findByUserName_ExistingUser_ReturnsOptionalPresent() {
        Optional<User> found = userRepository.findByUserName("johndoe");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("John Doe");
        assertThat(found.get().getEmail()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("findByUserName should return empty Optional when user does not exist")
    void findByUserName_NonExistingUser_ReturnsEmptyOptional() {
        Optional<User> found = userRepository.findByUserName("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByEmail should return present Optional when email exists")
    void findByEmail_ExistingEmail_ReturnsOptionalPresent() {
        Optional<User> found = userRepository.findByEmail("john.doe@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUserName()).isEqualTo("johndoe");
    }

    @Test
    @DisplayName("findByEmail should return empty Optional when email does not exist")
    void findByEmail_NonExistingEmail_ReturnsEmptyOptional() {
        Optional<User> found = userRepository.findByEmail("unknown@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByUserName should return true for existing username")
    void existsByUserName_ExistingUser_ReturnsTrue() {
        boolean exists = userRepository.existsByUserName("johndoe");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByUserName should return false for non-existing username")
    void existsByUserName_NonExistingUser_ReturnsFalse() {
        boolean exists = userRepository.existsByUserName("ghost_user");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("existsByEmail should return true for existing email")
    void existsByEmail_ExistingEmail_ReturnsTrue() {
        boolean exists = userRepository.existsByEmail("john.doe@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByEmail should return false for non-existing email")
    void existsByEmail_NonExistingEmail_ReturnsFalse() {
        boolean exists = userRepository.existsByEmail("notfound@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findByNameContaining should return matching users paged")
    void findByNameContaining_MatchingSubstring_ReturnsPagedUsers() {
        User anotherUser = User.builder()
                .name("Johnny Smith")
                .userName("johnny")
                .password("password")
                .email("johnny@example.com")
                .deleted(false)
                .build();
        entityManager.persistAndFlush(anotherUser);

        Pageable pageable = PageRequest.of(0, 10);
        Page<User> result = userRepository.findByNameContaining("John", pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(User::getName).containsExactlyInAnyOrder("John Doe", "Johnny Smith");
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase should match regardless of casing")
    void findByNameContainingIgnoreCase_CaseInsensitiveSubstring_ReturnsPagedUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> result = userRepository.findByNameContainingIgnoreCase("jOhN", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("findByNameContainingIgnoreCase should return empty page when no match found")
    void findByNameContainingIgnoreCase_NoMatch_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> result = userRepository.findByNameContainingIgnoreCase("Alexander", pageable);

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("persisting duplicate username should violate unique constraint")
    void save_DuplicateUserName_ThrowsException() {
        User duplicate = User.builder()
                .name("Different Name")
                .userName("johndoe") // Duplicate username
                .password("new_pass")
                .email("different@example.com")
                .deleted(false)
                .build();

        assertThatThrownBy(() -> entityManager.persistAndFlush(duplicate))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("persisting duplicate email should violate unique constraint")
    void save_DuplicateEmail_ThrowsException() {
        User duplicate = User.builder()
                .name("Another Name")
                .userName("differentuser")
                .password("new_pass")
                .email("john.doe@example.com") // Duplicate email
                .deleted(false)
                .build();

        assertThatThrownBy(() -> entityManager.persistAndFlush(duplicate))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("findById should retrieve user with deleted flag preserved")
    void findById_SoftDeletedUser_RetainsDeletedFlag() {
        User deletedUser = User.builder()
                .name("Inactive User")
                .userName("inactive")
                .password("password")
                .email("inactive@example.com")
                .deleted(true)
                .build();
        User saved = entityManager.persistAndFlush(deletedUser);

        Optional<User> found = userRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().isDeleted()).isTrue();
    }
}
