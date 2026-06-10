package wardrobe.project.com.userservice.service.user.impl;

import com.wardrobe.common.auth.AuthContext;
import com.wardrobe.common.auth.AuthContextHolder;
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
    public UserResponse syncCurrentUser() {
        AuthContext authContext = AuthContextHolder.get();

        String cognitoSub = authContext.requireUserId();
        String email = authContext.requireEmail();

        User user = userRepository.findById(cognitoSub)
                .orElseGet(() -> User.builder()
                        .userId(cognitoSub)
                        .status(UserStatus.ACTIVE)
                        .build());

        if (user.getEmail() == null || !user.getEmail().equals(email)) {
            user.setEmail(email);
        }

        User savedUser = userRepository.save(user);

        return userMapper.toUserResponse(savedUser);
    }
}
