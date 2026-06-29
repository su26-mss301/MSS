package wardrobe.project.com.userservice.service.user.Impl;

import com.wardrobe.common.auth.AuthContext;
import com.wardrobe.common.auth.AuthContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.userservice.dto.request.user.UpdateUserRequest;
import wardrobe.project.com.userservice.dto.response.auth.LoginResponse;
import wardrobe.project.com.userservice.dto.response.user.UserResponse;
import wardrobe.project.com.userservice.entity.User;
import wardrobe.project.com.userservice.entity.UserProfile;
import wardrobe.project.com.userservice.enums.Gender;
import wardrobe.project.com.userservice.mapper.UserMapper;
import wardrobe.project.com.userservice.repository.UserProfileRepository;
import wardrobe.project.com.userservice.repository.UserRepository;
import wardrobe.project.com.userservice.service.user.UserService;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserProfileRepository userProfileRepository;

    @Override
    public LoginResponse getCurrentUserByEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return LoginResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .role(user.getRole())
                .build();
    }

    @Override
    public UserResponse getMyInfo() {
        AuthContext authContext = AuthContextHolder.get();
        String userId = authContext.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseGet(() -> {
                    UserProfile newProfile = UserProfile.builder()
                            .user(user)
                            .build();

                    return userProfileRepository.save(newProfile);
                });

        return userMapper.toUserResponse(user, profile);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(UpdateUserRequest request) {
        AuthContext authContext = AuthContextHolder.get();
        String userId = authContext.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update bảng users
        if (hasText(request.getFullName())) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(emptyToNull(request.getAvatarUrl()));
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(emptyToNull(request.getPhoneNumber()));
        }

         if (request.getAddress() != null) {
             user.setAddress(emptyToNull(request.getAddress()));
         }

        userRepository.save(user);

        // Lấy hoặc tạo user profile
        UserProfile profile = userProfileRepository.findByUser(user)
                .orElseGet(() -> UserProfile.builder()
                        .user(user)
                        .build());

        // Update bảng user_profiles
        if (request.getGender() != null) {
            profile.setGender(parseGender(request.getGender()));
        }

        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(parseLocalDate(request.getDateOfBirth()));
        }

        if (request.getNotes() != null) {
            profile.setNote(emptyToNull(request.getNotes()));
        }

        if (request.getHeight() != null) {
            profile.setHeightCm(parseInteger(request.getHeight()));
        }

        if (request.getWeight() != null) {
            profile.setWeightKg(parseDouble(request.getWeight()));
        }

        if (request.getChest() != null) {
            profile.setChestCm(parseDouble(request.getChest()));
        }

        if (request.getWaist() != null) {
            profile.setWaistCm(parseDouble(request.getWaist()));
        }

        if (request.getHips() != null) {
            profile.setHipsCm(parseDouble(request.getHips()));
        }

        if (request.getShoeSize() != null) {
            profile.setShoeSize(parseDouble(request.getShoeSize()));
        }

        if (request.getFitPreference() != null) {
            profile.setFitPreference(emptyToNull(request.getFitPreference()));
        }

        if (request.getFavoriteColors() != null) {
            profile.setFavoriteColors(emptyToNull(request.getFavoriteColors()));
        }

        if (request.getStylePreference() != null) {
            profile.setStylePreference(emptyToNull(request.getStylePreference()));
        }

        if (request.getLifestylePreference() != null) {
            profile.setLifestylePreference(emptyToNull(request.getLifestylePreference()));
        }

        if (request.getOccupation() != null) {
            profile.setOccupation(emptyToNull(request.getOccupation()));
        }

        userProfileRepository.save(profile);

        return userMapper.toUserResponse(user, profile);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid integer value: " + value);
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid number value: " + value);
        }
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            throw new RuntimeException("Invalid date format. Expected yyyy-MM-dd");
        }
    }

    private Gender parseGender(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return Gender.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid gender: " + value);
        }
    }
}