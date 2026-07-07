package wardrobe.project.com.wardrobeservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.wardrobeservice.repository.ClothingItemRepository;
import wardrobe.project.com.wardrobeservice.repository.WardrobeRepository;
import wardrobe.project.com.wardrobeservice.repository.WardrobeZoneRepository;
import wardrobe.project.com.wardrobeservice.service.CleanupService;

@Service
@RequiredArgsConstructor
@Slf4j
public class CleanupServiceImpl implements CleanupService {

    private final WardrobeRepository wardrobeRepository;
    private final WardrobeZoneRepository wardrobeZoneRepository;
    private final ClothingItemRepository clothingItemRepository;

    @Scheduled(cron = "0 0 0 * * ?") // Chạy vào lúc 00:00 mỗi ngày
    @Transactional
    public void purgeOldDeletedData() {
        log.info("Bắt đầu dọn dẹp dữ liệu đã xóa mềm quá 30 ngày...");

        // Chúng ta sử dụng native query để xóa những dữ liệu bị xóa mềm quá 30 ngày.
        // Xóa quần áo trước
        clothingItemRepository.purgeOldDeletedItems();
        
        // Xóa ngăn kéo
        wardrobeZoneRepository.purgeOldDeletedZones();
        
        // Xóa tủ đồ
        wardrobeRepository.purgeOldDeletedWardrobes();

        log.info("Hoàn tất dọn dẹp dữ liệu.");
    }
}
