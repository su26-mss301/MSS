package wardrobe.project.com.userservice.controller.style;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.userservice.dto.ApiResponse;
import wardrobe.project.com.userservice.dto.request.style.SaveStylePreferenceRequest;
import wardrobe.project.com.userservice.dto.response.style.StylePreferenceResponse;
import wardrobe.project.com.userservice.service.style.UserStylePreferenceService;

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
}