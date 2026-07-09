package wardrobe.project.com.userservice.service.style.Impl;

import com.wardrobe.common.auth.AuthContext;
import com.wardrobe.common.auth.AuthContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.userservice.dto.request.style.SaveStylePreferenceRequest;
import wardrobe.project.com.userservice.dto.response.style.StylePreferenceResponse;
import wardrobe.project.com.userservice.entity.User;
import wardrobe.project.com.userservice.entity.UserStylePreference;
import wardrobe.project.com.userservice.mapper.UserStylePreferenceMapper;
import wardrobe.project.com.userservice.repository.UserRepository;
import wardrobe.project.com.userservice.repository.UserStylePreferenceRepository;
import wardrobe.project.com.userservice.service.style.UserStylePreferenceService;

@Service
@RequiredArgsConstructor
public class UserStylePreferenceServiceImpl implements UserStylePreferenceService {

    private final UserRepository userRepository;
    private final UserStylePreferenceRepository preferenceRepository;
    private final UserStylePreferenceMapper preferenceMapper;

    @Override
    @Transactional
    public StylePreferenceResponse saveMyPreferences(SaveStylePreferenceRequest request) {
        AuthContext authContext = AuthContextHolder.get();
        String email = authContext.requireEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        UserStylePreference preference = preferenceRepository.findByUser(user)
                .orElseGet(() -> UserStylePreference.builder()
                        .user(user)
                        .build());

        preference.setFavoriteColors(preferenceMapper.toJson(request.getFavoriteColors()));
        preference.setPreferredStyles(preferenceMapper.toJson(request.getPreferredStyles()));
        preference.setLifestyles(preferenceMapper.toJson(request.getLifestyles()));
        preference.setClothingInterests(preferenceMapper.toJson(request.getClothingInterests()));

        UserStylePreference saved = preferenceRepository.save(preference);

        return preferenceMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StylePreferenceResponse getMyPreferences() {
        AuthContext authContext = AuthContextHolder.get();
        String email = authContext.requireEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return preferenceRepository.findByUser(user)
                .map(preferenceMapper::toResponse)
                .orElseGet(() -> preferenceMapper.toResponse(null));
    }
}