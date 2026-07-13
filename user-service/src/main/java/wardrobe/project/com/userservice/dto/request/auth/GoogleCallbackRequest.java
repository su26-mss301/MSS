package wardrobe.project.com.userservice.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleCallbackRequest {

    @NotBlank(message = "Authorization code is required")
    private String code;

    @NotBlank(message = "Redirect URI is required")
    private String redirectUri;
}