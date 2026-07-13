package wardrobe.project.com.userservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    UNCATEGORIZED_EXCEPTION("Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    USER_ALREADY_EXISTS("User already exists", HttpStatus.CONFLICT),
    UNAUTHENTICATED("Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED("You do not have permission", HttpStatus.FORBIDDEN),

    PROFILE_NOT_FOUND("User profile not found", HttpStatus.NOT_FOUND),

    INVALID_REQUEST("Invalid request", HttpStatus.BAD_REQUEST),
    INVALID_DATE_FORMAT("Invalid date format. Expected yyyy-MM-dd", HttpStatus.BAD_REQUEST),
    INVALID_NUMBER_FORMAT("Invalid number format", HttpStatus.BAD_REQUEST),
    INVALID_GENDER("Invalid gender", HttpStatus.BAD_REQUEST),

    VALIDATION_ERROR("Validation error", HttpStatus.BAD_REQUEST),

    INVALID_USER_STATUS(
            "Invalid user status",
            HttpStatus.BAD_REQUEST
    ),

    INVALID_USER_ROLE(
            "Invalid user role",
            HttpStatus.BAD_REQUEST
    ),

    ROLE_UPDATE_NOT_ALLOWED(
            "You cannot update the role of a user.",
            HttpStatus.FORBIDDEN
    ),

    CANNOT_UPDATE_YOURSELF(
            "You cannot update your own information.",
            HttpStatus.FORBIDDEN
    );

    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}