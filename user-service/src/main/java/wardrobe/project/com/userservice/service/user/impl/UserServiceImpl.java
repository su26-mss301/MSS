package wardrobe.project.com.userservice.service.user.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.userservice.dto.response.user.UserResponse;
import wardrobe.project.com.userservice.entity.User;
import wardrobe.project.com.userservice.enums.UserStatus;
import wardrobe.project.com.userservice.mapper.UserMapper;
import wardrobe.project.com.userservice.repository.UserRepository;
import wardrobe.project.com.userservice.service.user.UserService;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    @Override
    @Transactional
    public UserResponse syncCurrentUser(Jwt jwt) {
        String userId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String username = jwt.getClaimAsString("cognito:username");

        User user = userRepository.findById(userId)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .userId(userId)
                            .email(email)
                            .username(username)
                            .fullName(username)
                            .status(UserStatus.ACTIVE)
                            .build();

                    return userRepository.save(newUser);
                });

        return userMapper.toUserResponse(user);
    }
}
