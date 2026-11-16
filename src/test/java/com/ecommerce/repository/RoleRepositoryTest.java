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

import com.ecommerce.model.Role;

@DataJpaTest
@DisplayName("RoleRepository JPA Slice Tests")
class RoleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoleRepository roleRepository;

    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
                .name("ROLE_USER")
                .description("Standard user role")
                .build();
        adminRole = Role.builder()
                .name("ROLE_ADMIN")
                .description("Administrator role")
                .build();

        entityManager.persistAndFlush(userRole);
        entityManager.persistAndFlush(adminRole);
    }

    @Test
    @DisplayName("findFirstByName should return present Optional for existing role")
    void findFirstByName_ExistingRole_ReturnsOptionalPresent() {
        Optional<Role> found = roleRepository.findFirstByName("ROLE_USER");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("ROLE_USER");
        assertThat(found.get().getDescription()).isEqualTo("Standard user role");
    }

    @Test
    @DisplayName("findFirstByName should return empty Optional for non-existing role")
    void findFirstByName_NonExistingRole_ReturnsEmptyOptional() {
        Optional<Role> found = roleRepository.findFirstByName("ROLE_MANAGER");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findByName should return list with matching role")
    void findByName_ExistingRole_ReturnsListWithMatchingRole() {
        List<Role> roles = roleRepository.findByName("ROLE_ADMIN");

        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getName()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("findByName should return empty list when role name not found")
    void findByName_NonExistingRole_ReturnsEmptyList() {
        List<Role> roles = roleRepository.findByName("ROLE_GUEST");

        assertThat(roles).isEmpty();
    }

    @Test
    @DisplayName("persisting duplicate role name should violate unique constraint")
    void save_DuplicateRoleName_ThrowsException() {
        Role duplicate = Role.builder()
                .name("ROLE_USER") // duplicate name
                .description("Duplicate role")
                .build();

        assertThatThrownBy(() -> entityManager.persistAndFlush(duplicate))
                .isInstanceOf(Exception.class);
    }
}
