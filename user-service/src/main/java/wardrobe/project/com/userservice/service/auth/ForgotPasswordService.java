package wardrobe.project.com.userservice.service.auth;

import wardrobe.project.com.userservice.dto.request.auth.ForgotPasswordRequest;
import wardrobe.project.com.userservice.dto.request.auth.VerifyForgotPasswordOtpRequest;
import wardrobe.project.com.userservice.dto.response.auth.ResetPasswordRequest;
import wardrobe.project.com.userservice.dto.response.auth.VerifyForgotPasswordOtpResponse;

public interface ForgotPasswordService {
    void forgotPassword(ForgotPasswordRequest request);

    VerifyForgotPasswordOtpResponse verifyOtp(VerifyForgotPasswordOtpRequest request);

    void resetPassword(ResetPasswordRequest request);
}
