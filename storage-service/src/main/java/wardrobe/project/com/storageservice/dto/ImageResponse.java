package wardrobe.project.com.storageservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import wardrobe.project.com.storageservice.model.ImageStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageResponse {
    private UUID id;
    private String name;
    private String url;
    private Float size;
    private ImageStatus status;
    private LocalDateTime createdAt;
}
