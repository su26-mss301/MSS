package wardrobe.project.com.userservice.service.style;

import wardrobe.project.com.userservice.dto.request.style.SaveStylePreferenceRequest;
import wardrobe.project.com.userservice.dto.response.style.StylePreferenceResponse;

public interface UserStylePreferenceService {
    StylePreferenceResponse saveMyPreferences(SaveStylePreferenceRequest request);
    StylePreferenceResponse getMyPreferences();
}
