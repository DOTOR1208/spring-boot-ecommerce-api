package com.ecommerce.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.ecommerce.dto.response.OrderDetailResponseDto;
import com.ecommerce.dto.response.OrderItemResponseDto;
import com.ecommerce.dto.response.OrderResponseDto;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.Orders;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "priceAtOrderTime", source = "priceAtOrderTime")
    OrderItemResponseDto toItemDTO(OrderItem orderItem);

    List<OrderItemResponseDto> toItemDTOs(List<OrderItem> orderItems);

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "orderDate", source = "orderDate")
    @Mapping(target = "status", expression = "java(orders.getStatus() != null ? orders.getStatus().name() : null)")
    @Mapping(target = "totalPrice", source = "orderItems", qualifiedByName = "calculateOrderTotal")
    OrderResponseDto toSummaryDTO(Orders orders);

    List<OrderResponseDto> toSummaryDTOs(List<Orders> ordersList);

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "orderDate", source = "orderDate")
    @Mapping(target = "status", expression = "java(orders.getStatus() != null ? orders.getStatus().name() : null)")
    @Mapping(target = "items", source = "orderItems")
    @Mapping(target = "totalPrice", source = "orderItems", qualifiedByName = "calculateOrderTotal")
    OrderDetailResponseDto toDetailDTO(Orders orders);

    @Named("calculateOrderTotal")
    default Double calculateOrderTotal(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return 0.0;
        }
        return items.stream()
                .filter(item -> item.getPriceAtOrderTime() != null && item.getQuantity() != null)
                .mapToDouble(item -> item.getPriceAtOrderTime() * item.getQuantity())
                .sum();
    }
}
