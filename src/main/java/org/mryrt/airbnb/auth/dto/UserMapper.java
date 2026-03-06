package org.mryrt.airbnb.auth.dto;

import org.mapstruct.Mapper;
import org.mryrt.airbnb.auth.model.User;


@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

}