package com.ecommerce.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import com.ecommerce.dto.response.CartItemResponseDto;
import com.ecommerce.dto.response.CartResponseDto;
import com.ecommerce.model.Cart;
import com.ecommerce.model.CartItem;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "price", source = "product.price")
    @Mapping(target = "quantity", source = "quantity")
    CartItemResponseDto toItemDTO(CartItem cartItem);

    List<CartItemResponseDto> toItemDTOs(List<CartItem> cartItems);

    @Mapping(target = "cartItems", source = "cartItems")
    @Mapping(target = "totalPrice", source = "cartItems", qualifiedByName = "calculateCartTotal")
    CartResponseDto toDTO(Cart cart);

    @Named("calculateCartTotal")
    default Double calculateCartTotal(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            return 0.0;
        }
        return items.stream()
                .filter(item -> item.getProduct() != null && item.getProduct().getPrice() != null && item.getQuantity() != null)
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();
    }
}
