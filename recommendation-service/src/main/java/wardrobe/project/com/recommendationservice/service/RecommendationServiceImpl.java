package wardrobe.project.com.recommendationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.recommendationservice.client.UserClient;
import wardrobe.project.com.recommendationservice.client.WardrobeClient;
import wardrobe.project.com.recommendationservice.dto.ClothingItemDto;
import wardrobe.project.com.recommendationservice.dto.UserProfileDto;
import wardrobe.project.com.recommendationservice.engine.OutfitGenerator;
import wardrobe.project.com.recommendationservice.engine.RecommendationEngine;
import wardrobe.project.com.recommendationservice.entity.Event;
import wardrobe.project.com.recommendationservice.entity.Outfit;
import wardrobe.project.com.recommendationservice.entity.RecommendItem;
import wardrobe.project.com.recommendationservice.repository.EventRepository;
import wardrobe.project.com.recommendationservice.repository.OutfitRepository;
import wardrobe.project.com.recommendationservice.repository.RecommendItemRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl {

    private final WardrobeClient wardrobeClient;
    private final UserClient userClient;
    private final RecommendationEngine engine;
    private final OutfitGenerator outfitGenerator;

    private final OutfitRepository outfitRepository;
    private final RecommendItemRepository recommendItemRepository;

    private final EventRepository eventRepository;

    // 1. CONTENT-BASED (WEEK 2)
    @Transactional
    public RecommendItem generateContentBased(UUID userId) {
        log.info("Generating Content-Based for User: {}", userId);
        UserProfileDto profile = userClient.getUserProfile(userId);
        List<ClothingItemDto> wardrobe = wardrobeClient.getUserClothes(userId);

        List<ClothingItemDto> rankedItems = engine.rankByContentBased(wardrobe, profile);
        List<ClothingItemDto> finalOutfit = outfitGenerator.generateBestOutfit(rankedItems);

        return saveRecommendation(userId, finalOutfit, null, 8.5f, "Gợi ý theo sở thích cá nhân");
    }

    // 2. EVENT-BASED (WEEK 3 TASK)
    @Transactional
    public RecommendItem generateEventBased(UUID userId, String eventType) {
        log.info("Generating Event-Based for User: {}, Event: {}", userId, eventType);
        List<ClothingItemDto> wardrobe = wardrobeClient.getUserClothes(userId);

        // Lọc đồ theo sự kiện
        List<ClothingItemDto> filteredItems = engine.filterByEvent(wardrobe, eventType);
        List<ClothingItemDto> finalOutfit = outfitGenerator.generateBestOutfit(filteredItems);

        // Tìm event trong DB để map khóa ngoại
        Event event = eventRepository.findByEventType(eventType).orElse(null);

        return saveRecommendation(userId, finalOutfit, event, 9.0f, "Gợi ý đi " + eventType);
    }

    // 3. COLLABORATIVE FILTERING (WEEK 4 TASK)
    @Transactional
    public RecommendItem generateCollaborative(UUID userId, UUID groupId) {
        log.info("Generating Collaborative for User: {}, Group: {}", userId, groupId);
        List<ClothingItemDto> wardrobe = wardrobeClient.getUserClothes(userId);

        // Giả lập lấy danh sách style thịnh hành từ nhóm bạn (Thực tế sẽ gọi UserClient)
        List<String> trendingStyles = List.of("Streetwear", "Vintage");

        List<ClothingItemDto> rankedItems = engine.rankByCollaborative(wardrobe, trendingStyles);
        List<ClothingItemDto> finalOutfit = outfitGenerator.generateBestOutfit(rankedItems);

        return saveRecommendation(userId, finalOutfit, null, 7.8f, "Gợi ý theo xu hướng nhóm bạn");
    }

    // Hàm dùng chung để lưu kết quả vào Database
    private RecommendItem saveRecommendation(UUID userId, List<ClothingItemDto> items, Event event, float score, String outfitName) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("Không đủ quần áo để phối thành bộ hợp lệ!");
        }

        Outfit outfit = new Outfit();
        outfit.setOutfitName(outfitName);
        outfit = outfitRepository.save(outfit);

        RecommendItem rec = new RecommendItem();
        rec.setUserId(userId);
        rec.setOutfit(outfit);
        rec.setEvent(event);
        rec.setRecommendationScore(score);

        return recommendItemRepository.save(rec);
    }
}