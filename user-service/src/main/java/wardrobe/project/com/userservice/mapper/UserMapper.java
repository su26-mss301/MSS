package wardrobe.project.com.userservice.mapper;

import org.mapstruct.Mapper;
import wardrobe.project.com.userservice.dto.response.user.UserResponse;
import wardrobe.project.com.userservice.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);
}