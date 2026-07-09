package wardrobe.project.com.userservice.dto.response.user;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    // users table
    private String userId;
    private String email;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String phoneNumber;
    private String address;
    private String role;
    private String status;

    // user_profiles table
    private String profileId;
    private String gender;
    private String dateOfBirth;
    private String notes;

    private String height;
    private String weight;
    private String chest;
    private String waist;
    private String hips;
    private String shoeSize;
    private String fitPreference;

    private String favoriteColors;
    private String stylePreference;
    private String lifestylePreference;
    private String occupation;
}