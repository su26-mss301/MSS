package wardrobe.project.com.userservice.controller.style;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.userservice.dto.ApiResponse;
import wardrobe.project.com.userservice.dto.request.style.SaveStylePreferenceRequest;
import wardrobe.project.com.userservice.dto.response.style.StylePreferenceResponse;
import wardrobe.project.com.userservice.service.style.UserStylePreferenceService;

import java.util.UUID;

@RestController
@RequestMapping("/style-preferences")
@RequiredArgsConstructor
public class UserStylePreferenceController {

    private final UserStylePreferenceService preferenceService;

    @GetMapping("/me")
    public ApiResponse<StylePreferenceResponse> getMyPreferences() {
        StylePreferenceResponse response = preferenceService.getMyPreferences();

        return ApiResponse.success(
                "Style preferences retrieved successfully",
                response
        );
    }

    @PutMapping("/me")
    public ApiResponse<StylePreferenceResponse> saveMyPreferences(
            @RequestBody SaveStylePreferenceRequest request
    ) {
        StylePreferenceResponse response = preferenceService.saveMyPreferences(request);

        return ApiResponse.success(
                "Style preferences saved successfully",
                response
        );
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<StylePreferenceResponse> getUserPreferences(@PathVariable UUID userId) {
        return ApiResponse.success(
                "User style preferences retrieved successfully",
                preferenceService.getUserPreferences(userId)
        );
    }
}