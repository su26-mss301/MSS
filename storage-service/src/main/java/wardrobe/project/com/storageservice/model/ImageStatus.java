package wardrobe.project.com.storageservice.model;

public enum ImageStatus {
    DETECTING,  // Ảnh vừa được upload, chờ user xác nhận thêm vào tủ đồ
    DONE        // Ảnh đã được xác nhận, chính thức lưu vào tủ đồ
}
