package wardrobe.project.com.userservice.service.auth.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import wardrobe.project.com.userservice.service.auth.EmailService;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendRegisterOtp(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Smart Wardrobe - Mã xác thực đăng ký");
        message.setText("""
                Xin chào,

                Mã OTP xác thực đăng ký tài khoản Smart Wardrobe của bạn là:

                %s

                Mã này có hiệu lực trong 5 phút.

                Nếu bạn không đăng ký tài khoản, vui lòng bỏ qua email này.
                """.formatted(otp));

        mailSender.send(message);
    }
}