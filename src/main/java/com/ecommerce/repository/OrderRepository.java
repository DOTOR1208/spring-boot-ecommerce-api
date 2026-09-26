package com.ecommerce.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.model.Orders;
import com.ecommerce.model.User;

@Repository
public interface OrderRepository extends JpaRepository<Orders, Integer> {

    List<Orders> findByUser(User user);

    List<Orders> findByUserOrderByOrderDateDesc(User user);

    List<Orders> findByUserId(Integer userId);
}
