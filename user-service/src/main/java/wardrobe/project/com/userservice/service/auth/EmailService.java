package wardrobe.project.com.userservice.service.auth;

public interface EmailService {
    void sendRegisterOtp(String to, String otp);
}
