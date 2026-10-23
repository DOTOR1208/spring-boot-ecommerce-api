package com.ecommerce.service;

import java.util.List;

import com.ecommerce.dto.response.OrderDetailResponseDto;
import com.ecommerce.dto.response.OrderResponseDto;
import com.ecommerce.model.User;

public interface OrderService {

    OrderResponseDto checkoutOrder(User user);

    List<OrderResponseDto> getOrderSummaries(User user);

    OrderDetailResponseDto getOrderDetail(Integer orderId, User user);
}
