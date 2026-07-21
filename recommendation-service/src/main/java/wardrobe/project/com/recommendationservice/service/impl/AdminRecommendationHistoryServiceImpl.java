package wardrobe.project.com.recommendationservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.recommendationservice.dto.response.OutfitResponseDTO;
import wardrobe.project.com.recommendationservice.dto.response.RecommendationHistoryDetailDTO;
import wardrobe.project.com.recommendationservice.dto.response.RecommendationHistoryItemDTO;
import wardrobe.project.com.recommendationservice.dto.response.RecommendationHistoryPageResponse;
import wardrobe.project.com.recommendationservice.dto.response.RecommendationMemberOutfitDTO;
import wardrobe.project.com.recommendationservice.entity.Outfit;
import wardrobe.project.com.recommendationservice.entity.RecommendItem;
import wardrobe.project.com.recommendationservice.entity.RecommendMemberOutfit;
import wardrobe.project.com.recommendationservice.repository.OutfitItemRepository;
import wardrobe.project.com.recommendationservice.repository.RecommendItemRepository;
import wardrobe.project.com.recommendationservice.repository.RecommendMemberOutfitRepository;
import wardrobe.project.com.recommendationservice.service.AdminRecommendationHistoryService;
import wardrobe.project.com.recommendationservice.service.RecommendationServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminRecommendationHistoryServiceImpl implements AdminRecommendationHistoryService {

    private final RecommendItemRepository recommendItemRepository;
    private final RecommendMemberOutfitRepository recommendMemberOutfitRepository;
    private final OutfitItemRepository outfitItemRepository;
    private final RecommendationServiceImpl recommendationService;

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

    @Override
    @Transactional(readOnly = true)
    public RecommendationHistoryDetailDTO getDetail(UUID recommendationId) {
        RecommendItem item = recommendItemRepository.findById(recommendationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy gợi ý với ID: " + recommendationId));

        Outfit mainOutfit = item.getOutfit();
        String recommendationType = resolveRecommendationType(item, mainOutfit != null ? mainOutfit.getOutfitName() : null);
        OutfitResponseDTO mainOutfitDto = recommendationService.buildOutfitResponseForAdmin(mainOutfit, item);

        List<RecommendationMemberOutfitDTO> members = new ArrayList<>();
        List<RecommendMemberOutfit> memberOutfits =
                recommendMemberOutfitRepository.findByRecommendItemOrderByCreatedAtAsc(item);
        if ("group".equals(recommendationType)) {
            members.addAll(recommendationService.buildAdminGroupMemberDetails(item, memberOutfits));
        } else {
            for (RecommendMemberOutfit memberOutfit : memberOutfits) {
                OutfitResponseDTO memberOutfitDto = recommendationService.buildOutfitResponseForAdmin(
                        memberOutfit.getOutfit(),
                        item
                );
                members.add(RecommendationMemberOutfitDTO.builder()
                        .userId(memberOutfit.getMemberUserId())
                        .fullName(memberOutfit.getMemberName())
                        .recommendationScore(memberOutfit.getMemberScore())
                        .outfit(memberOutfitDto)
                        .creator(item.getUserId().equals(memberOutfit.getMemberUserId()))
                        .build());
            }
        }

        return RecommendationHistoryDetailDTO.builder()
                .recommendationId(item.getId())
                .userId(item.getUserId())
                .recommendationType(recommendationType)
                .eventType(item.getEvent() != null ? item.getEvent().getEventType() : "General")
                .recommendationScore(item.getRecommendationScore())
                .generatedAt(item.getGeneratedAt())
                .outfit(normalizeGroupOutfitName(mainOutfitDto, item, recommendationType))
                .groupId(item.getGroupId())
                .groupName(item.getGroupName())
                .groupStyles(recommendationService.parseGroupStyles(item.getGroupStyles()))
                .members(members)
                .build();
    }

    private OutfitResponseDTO normalizeGroupOutfitName(
            OutfitResponseDTO outfitDto,
            RecommendItem item,
            String recommendationType
    ) {
        if (!"group".equals(recommendationType) || outfitDto == null) {
            return outfitDto;
        }
        String displayName = resolveDisplayOutfitName(item, outfitDto.getOutfitName(), recommendationType);
        outfitDto.setOutfitName(displayName);
        return outfitDto;
    }

    private RecommendationHistoryItemDTO toHistoryItem(RecommendItem item) {
        Outfit outfit = item.getOutfit();
        String rawOutfitName = outfit != null ? outfit.getOutfitName() : null;
        String recommendationType = resolveRecommendationType(item, rawOutfitName);
        String outfitName = resolveDisplayOutfitName(item, rawOutfitName, recommendationType);
        int itemCount = outfit != null
                ? (int) outfitItemRepository.countByOutfit(outfit)
                : 0;

        return RecommendationHistoryItemDTO.builder()
                .recommendationId(item.getId())
                .userId(item.getUserId())
                .outfitName(outfitName)
                .groupName(item.getGroupName())
                .description(outfit != null ? outfit.getDescription() : null)
                .itemCount(itemCount)
                .recommendationScore(item.getRecommendationScore())
                .eventType(item.getEvent() != null ? item.getEvent().getEventType() : "General")
                .recommendationType(recommendationType)
                .generatedAt(item.getGeneratedAt())
                .build();
    }

    private String resolveDisplayOutfitName(RecommendItem item, String rawOutfitName, String recommendationType) {
        if ("group".equals(recommendationType)) {
            if (item.getGroupName() != null && !item.getGroupName().isBlank()) {
                return item.getGroupName() + " (Nhóm)";
            }
            return normalizeLegacyGroupOutfitName(rawOutfitName);
        }
        return rawOutfitName != null ? rawOutfitName : "—";
    }

    private String normalizeLegacyGroupOutfitName(String rawOutfitName) {
        if (rawOutfitName == null || rawOutfitName.isBlank()) {
            return "—";
        }
        String name = rawOutfitName;
        if (name.startsWith("Phong Cách ")) {
            name = name.substring("Phong Cách ".length());
        }
        int memberSuffixIndex = name.indexOf(" · ");
        if (memberSuffixIndex > 0) {
            name = name.substring(0, memberSuffixIndex);
        }
        return name.trim();
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

    private String resolveRecommendationType(RecommendItem item, String outfitName) {
        if (item.getGroupId() != null) {
            return "group";
        }
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
