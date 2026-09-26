package com.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.model.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    List<Role> findByName(String name);

    Optional<Role> findFirstByName(String name);
}
