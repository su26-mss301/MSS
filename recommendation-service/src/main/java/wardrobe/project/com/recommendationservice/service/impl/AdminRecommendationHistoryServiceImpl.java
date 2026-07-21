package wardrobe.project.com.recommendationservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.recommendationservice.dto.response.RecommendationHistoryItemDTO;
import wardrobe.project.com.recommendationservice.dto.response.RecommendationHistoryPageResponse;
import wardrobe.project.com.recommendationservice.entity.Outfit;
import wardrobe.project.com.recommendationservice.entity.RecommendItem;
import wardrobe.project.com.recommendationservice.repository.OutfitItemRepository;
import wardrobe.project.com.recommendationservice.repository.RecommendItemRepository;
import wardrobe.project.com.recommendationservice.service.AdminRecommendationHistoryService;

import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminRecommendationHistoryServiceImpl implements AdminRecommendationHistoryService {

    private final RecommendItemRepository recommendItemRepository;
    private final OutfitItemRepository outfitItemRepository;

    @Override
    @Transactional(readOnly = true)
    public RecommendationHistoryPageResponse getHistory(
            String type,
            int page,
            int size,
            String sort
    ) {
        String normalizedType = normalizeType(type);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Sort sortOrder = "oldest".equalsIgnoreCase(sort)
                ? Sort.by("generatedAt").ascending()
                : Sort.by("generatedAt").descending();

        Page<RecommendItem> result = recommendItemRepository.findHistory(
                normalizedType,
                PageRequest.of(safePage, safeSize, sortOrder)
        );

        List<RecommendationHistoryItemDTO> items = result.getContent().stream()
                .map(this::toHistoryItem)
                .toList();

        return RecommendationHistoryPageResponse.builder()
                .items(items)
                .page(result.getNumber())
                .size(result.getSize())
                .totalItems(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .first(result.isFirst())
                .last(result.isLast())
                .hasNext(result.hasNext())
                .hasPrevious(result.hasPrevious())
                .totalCount(recommendItemRepository.count())
                .personalCount(recommendItemRepository.countPersonal())
                .groupCount(recommendItemRepository.countGroup())
                .eventCount(recommendItemRepository.countEvent())
                .build();
    }

    private RecommendationHistoryItemDTO toHistoryItem(RecommendItem item) {
        Outfit outfit = item.getOutfit();
        String outfitName = outfit != null ? outfit.getOutfitName() : "—";
        int itemCount = outfit != null
                ? (int) outfitItemRepository.countByOutfit(outfit)
                : 0;

        return RecommendationHistoryItemDTO.builder()
                .recommendationId(item.getId())
                .userId(item.getUserId())
                .outfitName(outfitName)
                .description(outfit != null ? outfit.getDescription() : null)
                .itemCount(itemCount)
                .recommendationScore(item.getRecommendationScore())
                .eventType(item.getEvent() != null ? item.getEvent().getEventType() : "General")
                .recommendationType(resolveRecommendationType(outfitName))
                .generatedAt(item.getGeneratedAt())
                .build();
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "all";
        }
        return switch (type.toLowerCase()) {
            case "personal", "group", "event" -> type.toLowerCase();
            default -> "all";
        };
    }

    private String resolveRecommendationType(String outfitName) {
        if (outfitName == null) {
            return "event";
        }
        String lower = outfitName.toLowerCase();
        if (lower.contains("cá nhân")) {
            return "personal";
        }
        if (lower.contains("nhóm")) {
            return "group";
        }
        return "event";
    }
}
