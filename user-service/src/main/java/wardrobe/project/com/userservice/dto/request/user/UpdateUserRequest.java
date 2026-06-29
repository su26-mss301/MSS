package wardrobe.project.com.userservice.dto.request.user;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    // users table
    private String fullName;
    private String avatarUrl;
    private String phoneNumber;
    private String address;

    // user_profiles table
    private String dateOfBirth;
    private String gender;
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