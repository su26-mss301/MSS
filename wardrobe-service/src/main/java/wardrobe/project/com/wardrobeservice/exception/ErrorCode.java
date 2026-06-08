package wardrobe.project.com.wardrobeservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INTERNAL_SERVER_ERROR("Đã xảy ra lỗi hệ thống", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT("Dữ liệu đầu vào không hợp lệ", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR("Lỗi xác thực dữ liệu", HttpStatus.BAD_REQUEST),
    
    WARDROBE_NOT_FOUND("Không tìm thấy tủ quần áo", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND("Không tìm thấy thể loại", HttpStatus.NOT_FOUND),
    WARDROBE_ZONE_NOT_FOUND("Không tìm thấy ngăn tủ", HttpStatus.NOT_FOUND),
    CLOTHING_ITEM_NOT_FOUND("Không tìm thấy quần áo", HttpStatus.NOT_FOUND),
    
    USER_ID_BLANK("User ID không được để trống", HttpStatus.BAD_REQUEST),
    WARDROBE_NAME_BLANK("Tên tủ quần áo không được để trống", HttpStatus.BAD_REQUEST),
    WARDROBE_NAME_SIZE("Tên tủ quần áo không được vượt quá 100 ký tự", HttpStatus.BAD_REQUEST),
    CATEGORY_NAME_BLANK("Tên thể loại không được để trống", HttpStatus.BAD_REQUEST),
    CATEGORY_NAME_SIZE("Tên thể loại không được vượt quá 100 ký tự", HttpStatus.BAD_REQUEST),
    WARDROBE_ID_BLANK("ID của tủ quần áo không được để trống", HttpStatus.BAD_REQUEST),
    ZONE_NAME_BLANK("Tên ngăn tủ không được để trống", HttpStatus.BAD_REQUEST),
    ZONE_NAME_SIZE("Tên ngăn tủ không được vượt quá 100 ký tự", HttpStatus.BAD_REQUEST),
    ITEM_NAME_BLANK("Tên quần áo không được để trống", HttpStatus.BAD_REQUEST),
    ITEM_NAME_SIZE("Tên quần áo không được vượt quá 100 ký tự", HttpStatus.BAD_REQUEST),
    COLOR_SIZE("Màu sắc không được vượt quá 50 ký tự", HttpStatus.BAD_REQUEST),
    STYLE_SIZE("Phong cách không được vượt quá 50 ký tự", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
