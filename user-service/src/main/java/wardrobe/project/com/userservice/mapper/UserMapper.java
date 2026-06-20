package wardrobe.project.com.userservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import wardrobe.project.com.userservice.dto.response.auth.LoginResponse;
import wardrobe.project.com.userservice.dto.response.user.UserResponse;
import wardrobe.project.com.userservice.entity.User;
import wardrobe.project.com.userservice.enums.Role;
import wardrobe.project.com.userservice.enums.UserStatus;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);

    @Mapping(target = "email", source = "email")
    @Mapping(target = "username", source = "username")
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "avatarUrl", source = "avatarUrl")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "role", source = "role")
    LoginResponse toLoginResponse(User user);

}