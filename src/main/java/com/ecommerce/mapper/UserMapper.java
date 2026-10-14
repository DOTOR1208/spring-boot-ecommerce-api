package com.ecommerce.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.ecommerce.dto.request.SignUpRequest;
import com.ecommerce.dto.response.UserResponseDto;
import com.ecommerce.model.Role;
import com.ecommerce.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "cart", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "roles", ignore = true)
    User toModel(SignUpRequest dto);

    @Mapping(target = "roles", expression = "java(mapRoles(user))")
    UserResponseDto toDTO(User user);

    default List<String> mapRoles(User user) {
        if (user == null || user.getRoles() == null) {
            return new ArrayList<>();
        }
        return user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());
    }

    List<UserResponseDto> toDTOs(List<User> users);
}
