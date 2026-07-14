package wardrobe.project.com.wardrobeservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wardrobe.project.com.wardrobeservice.dto.request.CategoryCreateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.request.CategoryUpdateRequestDTO;
import wardrobe.project.com.wardrobeservice.dto.response.CategoryAnalyticsItemDTO;
import wardrobe.project.com.wardrobeservice.dto.response.CategoryAnalyticsResponseDTO;
import wardrobe.project.com.wardrobeservice.dto.response.CategoryResponseDTO;
import wardrobe.project.com.wardrobeservice.dto.response.CategoryUserItemDTO;
import wardrobe.project.com.wardrobeservice.dto.response.CategoryUsersResponseDTO;
import wardrobe.project.com.wardrobeservice.entity.Category;
import wardrobe.project.com.wardrobeservice.exception.AppException;
import wardrobe.project.com.wardrobeservice.exception.ErrorCode;
import wardrobe.project.com.wardrobeservice.repository.CategoryRepository;
import wardrobe.project.com.wardrobeservice.repository.ClothingItemRepository;
import wardrobe.project.com.wardrobeservice.service.CategoryService;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    /** 13 danh mục AI — khớp CATEGORY_LABELS_VI bên FE */
    private static final List<String> AI_CATEGORY_NAMES = List.of(
            "Áo tay ngắn", "Áo tay dài", "Áo khoác tay ngắn", "Áo khoác tay dài",
            "Áo gile", "Áo hai dây", "Quần short", "Quần dài", "Chân váy",
            "Đầm tay ngắn", "Đầm tay dài", "Đầm gile", "Đầm hai dây"
    );

    private final CategoryRepository categoryRepository;
    private final ClothingItemRepository clothingItemRepository;

    @Override
    public CategoryResponseDTO createCategory(CategoryCreateRequestDTO request) {
        Category category = Category.builder()
                .categoryName(request.getCategoryName())
                .description(request.getDescription())
                .build();
        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    @Override
    public CategoryResponseDTO getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return mapToResponse(category);
    }

    @Override
    public List<CategoryResponseDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponseDTO updateCategory(UUID id, CategoryUpdateRequestDTO request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());

        Category updatedCategory = categoryRepository.save(category);
        return mapToResponse(updatedCategory);
    }

    @Override
    public void deleteCategory(UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        categoryRepository.deleteById(id);
    }

    @Override
    public CategoryAnalyticsResponseDTO getCategoryAnalytics(String granularity, String date) {
        DateRange range = resolveDateRange(granularity, date);
        List<Category> allCategories = categoryRepository.findAll();
        List<Object[]> counts = clothingItemRepository.countByCategoryAndDateRange(range.from(), range.to());

        Map<UUID, Long> countMap = new HashMap<>();
        for (Object[] row : counts) {
            countMap.put((UUID) row[0], (Long) row[1]);
        }

        // Gộp count theo tên danh mục (xử lý bản ghi trùng trong DB)
        Map<String, Long> countByName = new HashMap<>();
        Map<String, UUID> idByName = new HashMap<>();
        for (Category cat : allCategories) {
            String key = normalizeCategoryName(cat.getCategoryName());
            long count = countMap.getOrDefault(cat.getCategoryId(), 0L);
            countByName.merge(key, count, Long::sum);
            idByName.putIfAbsent(key, cat.getCategoryId());
        }

        long total = countByName.values().stream().mapToLong(Long::longValue).sum();

        List<CategoryAnalyticsItemDTO> items = new ArrayList<>();
        for (String canonical : AI_CATEGORY_NAMES) {
            String key = normalizeCategoryName(canonical);
            long count = countByName.getOrDefault(key, 0L);
            double pct = total > 0 ? Math.round((count * 10000.0 / total)) / 100.0 : 0.0;
            items.add(CategoryAnalyticsItemDTO.builder()
                    .categoryId(idByName.getOrDefault(key, UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8))))
                    .categoryName(canonical)
                    .count(count)
                    .percentage(pct)
                    .build());
        }

        items.sort(Comparator.comparingLong(CategoryAnalyticsItemDTO::getCount).reversed());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return CategoryAnalyticsResponseDTO.builder()
                .granularity(range.granularity())
                .from(range.from().format(fmt))
                .to(range.to().minusSeconds(1).format(fmt))
                .totalItems(total)
                .categories(items)
                .build();
    }

    @Override
    public CategoryUsersResponseDTO getCategoryUsers(String categoryName, String granularity, String date) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new AppException(ErrorCode.CATEGORY_NAME_BLANK);
        }

        String targetKey = normalizeCategoryName(categoryName);
        boolean isValid = AI_CATEGORY_NAMES.stream()
                .anyMatch(name -> normalizeCategoryName(name).equals(targetKey));
        if (!isValid) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        DateRange range = resolveDateRange(granularity, date);

        List<UUID> categoryIds = categoryRepository.findAll().stream()
                .filter(cat -> normalizeCategoryName(cat.getCategoryName()).equals(targetKey))
                .map(Category::getCategoryId)
                .distinct()
                .toList();

        if (categoryIds.isEmpty()) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
            return CategoryUsersResponseDTO.builder()
                    .categoryName(categoryName)
                    .granularity(range.granularity())
                    .from(range.from().format(fmt))
                    .to(range.to().minusSeconds(1).format(fmt))
                    .totalUsers(0)
                    .totalItems(0)
                    .users(List.of())
                    .build();
        }

        List<Object[]> rows = clothingItemRepository.countByCategoryIdsGroupByUser(
                categoryIds, range.from(), range.to());

        DateTimeFormatter itemFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        List<CategoryUserItemDTO> users = new ArrayList<>();
        long totalItems = 0;
        for (Object[] row : rows) {
            UUID userId = (UUID) row[0];
            long count = (Long) row[1];
            LocalDateTime firstAdded = (LocalDateTime) row[2];
            LocalDateTime lastAdded = (LocalDateTime) row[3];
            totalItems += count;
            users.add(CategoryUserItemDTO.builder()
                    .userId(userId)
                    .itemCount(count)
                    .firstAddedAt(firstAdded != null ? firstAdded.format(itemFmt) : null)
                    .lastAddedAt(lastAdded != null ? lastAdded.format(itemFmt) : null)
                    .build());
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return CategoryUsersResponseDTO.builder()
                .categoryName(categoryName)
                .granularity(range.granularity())
                .from(range.from().format(fmt))
                .to(range.to().minusSeconds(1).format(fmt))
                .totalUsers(users.size())
                .totalItems(totalItems)
                .users(users)
                .build();
    }

    private DateRange resolveDateRange(String granularity, String date) {
        LocalDate parsedDate = (date != null && !date.isBlank())
                ? LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
                : LocalDate.now();

        String g = granularity == null ? "month" : granularity.toLowerCase();
        LocalDateTime from;
        LocalDateTime to;

        switch (g) {
            case "day" -> {
                from = parsedDate.atStartOfDay();
                to = parsedDate.plusDays(1).atStartOfDay();
            }
            case "year" -> {
                from = parsedDate.withDayOfYear(1).atStartOfDay();
                to = parsedDate.withDayOfYear(1).plusYears(1).atStartOfDay();
            }
            default -> {
                from = parsedDate.withDayOfMonth(1).atStartOfDay();
                to = parsedDate.withDayOfMonth(1).plusMonths(1).atStartOfDay();
                g = "month";
            }
        }

        return new DateRange(g, from, to);
    }

    private record DateRange(String granularity, LocalDateTime from, LocalDateTime to) {}

    private CategoryResponseDTO mapToResponse(Category category) {
        return CategoryResponseDTO.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .build();
    }

    /** Chuẩn hóa tên danh mục để gộp bản ghi trùng (bỏ dấu, lowercase) */
    private String normalizeCategoryName(String name) {
        if (name == null) return "";
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        return normalized
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
