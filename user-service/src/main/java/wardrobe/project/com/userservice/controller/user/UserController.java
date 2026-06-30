package wardrobe.project.com.userservice.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import wardrobe.project.com.userservice.dto.ApiResponse;
import wardrobe.project.com.userservice.dto.request.user.UpdateUserRequest;
import wardrobe.project.com.userservice.dto.response.user.UserResponse;
import wardrobe.project.com.userservice.service.user.UserService;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.success(userService.getMyInfo());
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateMyInfo(@RequestBody UpdateUserRequest request) {
        return ApiResponse.success(
                "Update profile successfully",
                userService.updateProfile(request)
        );
    }

    @PutMapping(
            value = "/me/avatar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<UserResponse> uploadMyAvatar(
            @RequestPart("file") MultipartFile file
    ) {


        UserResponse response = userService.uploadAvatar(file);

        return ApiResponse.success(response);
    }
}
