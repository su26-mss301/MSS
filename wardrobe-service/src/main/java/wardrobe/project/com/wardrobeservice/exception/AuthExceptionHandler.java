package wardrobe.project.com.wardrobeservice.exception;

import com.wardrobe.common.auth.GlobalAuthExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class AuthExceptionHandler extends GlobalAuthExceptionHandler {
}