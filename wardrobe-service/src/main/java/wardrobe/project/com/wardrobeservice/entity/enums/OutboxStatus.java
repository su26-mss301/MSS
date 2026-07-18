package wardrobe.project.com.wardrobeservice.entity.enums;

public enum OutboxStatus {

    /**
     * Event vừa được tạo trong cùng transaction nghiệp vụ,
     * chưa được Outbox Publisher lấy ra xử lý.
     */
    PENDING,

    /**
     * Event đã được một instance Wardrobe Service nhận xử lý.
     *
     * Trạng thái này giúp hạn chế hai instance cùng publish
     * một outbox event tại cùng thời điểm.
     */
    PROCESSING,

    /**
     * Kafka broker đã xác nhận gửi thành công.
     */
    PUBLISHED,

    /**
     * Lần publish gần nhất thất bại.
     *
     * Event sẽ được retry khi nextRetryAt đến hạn.
     */
    FAILED
}