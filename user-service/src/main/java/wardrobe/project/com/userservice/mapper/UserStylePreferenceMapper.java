package wardrobe.project.com.userservice.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import wardrobe.project.com.userservice.dto.response.style.StylePreferenceResponse;
import wardrobe.project.com.userservice.entity.UserStylePreference;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserStylePreferenceMapper {

    private final ObjectMapper objectMapper;

    public StylePreferenceResponse toResponse(UserStylePreference preference) {
        if (preference == null) {
            return StylePreferenceResponse.builder()
                    .favoriteColors(List.of())
                    .preferredStyles(List.of())
                    .lifestyles(List.of())
                    .clothingInterests(List.of())
                    .build();
        }

        return StylePreferenceResponse.builder()
                .preferenceId(preference.getPreferenceId())
                .favoriteColors(fromJson(preference.getFavoriteColors()))
                .preferredStyles(fromJson(preference.getPreferredStyles()))
                .lifestyles(fromJson(preference.getLifestyles()))
                .clothingInterests(fromJson(preference.getClothingInterests()))
                .updatedAt(preference.getUpdatedAt())
                .build();
    }

    public String toJson(List<String> values) {
        try {
            if (values == null) {
                values = List.of();
            }

            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            throw new RuntimeException("Không thể chuyển danh sách sở thích sang JSON");
        }
    }

    public List<String> fromJson(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new ArrayList<>();
            }

            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}