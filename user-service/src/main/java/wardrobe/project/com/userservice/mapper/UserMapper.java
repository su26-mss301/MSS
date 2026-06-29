package wardrobe.project.com.userservice.mapper;

import org.springframework.stereotype.Component;
import wardrobe.project.com.userservice.dto.response.user.UserResponse;
import wardrobe.project.com.userservice.entity.User;
import wardrobe.project.com.userservice.entity.UserProfile;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .phoneNumber(user.getPhoneNumber())
                 .address(user.getAddress())
                 .role(user.getRole() != null ? user.getRole().name() : null)
                 .status(user.getStatus() != null ? user.getStatus().name() : null)
                .build();
    }

    public UserResponse toUserResponse(User user, UserProfile profile) {
        if (user == null) {
            return null;
        }

        UserResponse.UserResponseBuilder builder = UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .phoneNumber(user.getPhoneNumber());

        // Nếu User có address, role, status thì mở ra
         builder.address(user.getAddress());
         builder.role(user.getRole() != null ? user.getRole().name() : null);
         builder.status(user.getStatus() != null ? user.getStatus().name() : null);

        if (profile != null) {
            builder
                    .profileId(profile.getProfileId())
                    .gender(profile.getGender() != null ? profile.getGender().name() : null)
                    .dateOfBirth(profile.getDateOfBirth() != null ? profile.getDateOfBirth().toString() : null)
                    .notes(profile.getNote())

                    .height(profile.getHeightCm() != null ? profile.getHeightCm().toString() : null)
                    .weight(profile.getWeightKg() != null ? profile.getWeightKg().toString() : null)

                    .chest(profile.getChestCm() != null ? profile.getChestCm().toString() : null)
                    .waist(profile.getWaistCm() != null ? profile.getWaistCm().toString() : null)
                    .hips(profile.getHipsCm() != null ? profile.getHipsCm().toString() : null)
                    .shoeSize(profile.getShoeSize() != null ? profile.getShoeSize().toString() : null)
                    .fitPreference(profile.getFitPreference())

                    .favoriteColors(profile.getFavoriteColors())
                    .stylePreference(profile.getStylePreference())
                    .lifestylePreference(profile.getLifestylePreference())
                    .occupation(profile.getOccupation());
        }

        return builder.build();
    }
}