package wardrobe.project.com.wardrobeservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "wardrobe_zone")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WardrobeZone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "zone_id", updatable = false, nullable = false)
    private UUID zoneId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wardrobe_id", nullable = false)
    private Wardrobe wardrobe;

    @Column(name = "zone_name", nullable = false)
    private String zoneName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}
