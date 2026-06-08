package wardrobe.project.com.userservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        onCreate();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
        onUpdate();
    }

    protected void onCreate() {
    }

    protected void onUpdate() {
    }
}
