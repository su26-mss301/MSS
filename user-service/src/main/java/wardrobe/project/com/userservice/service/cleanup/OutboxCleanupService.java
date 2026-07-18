package wardrobe.project.com.userservice.service.cleanup;

public interface OutboxCleanupService {

    int cleanupSentEvents();
}