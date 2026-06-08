package wardrobe.project.com.userservice.service.auth.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import wardrobe.project.com.userservice.dto.response.auth.CognitoLoginResponse;
import wardrobe.project.com.userservice.service.auth.CognitoAuthService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CognitoAuthServiceImpl implements CognitoAuthService {

    private final CognitoIdentityProviderClient cognitoClient;

    @Value("${aws.cognito.client-id}")
    private String clientId;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Override
    public CognitoLoginResponse login(String email, String password) {
        InitiateAuthRequest request = InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .clientId(clientId)
                .authParameters(Map.of(
                        "USERNAME", email,
                        "PASSWORD", password
                ))
                .build();

        var response = cognitoClient.initiateAuth(request);
        var result = response.authenticationResult();

        return new CognitoLoginResponse(
                result.accessToken(),
                result.idToken(),
                result.refreshToken()
        );
    }

    @Override
    public void register(String email, String password) {
        SignUpRequest request = SignUpRequest.builder()
                .clientId(clientId)
                .username(email)
                .password(password)
                .userAttributes(
                        AttributeType.builder()
                                .name("email")
                                .value(email)
                                .build()
                )
                .build();

        cognitoClient.signUp(request);
    }

    @Override
    public void confirmRegister(String email, String otp) {
        ConfirmSignUpRequest request = ConfirmSignUpRequest.builder()
                .clientId(clientId)
                .username(email)
                .confirmationCode(otp)
                .build();

        cognitoClient.confirmSignUp(request);

        AdminAddUserToGroupRequest addToGroupRequest =
                AdminAddUserToGroupRequest.builder()
                        .userPoolId(userPoolId)
                        .username(email)
                        .groupName("ROLE_USER")
                        .build();

        cognitoClient.adminAddUserToGroup(addToGroupRequest);
    }

    @Override
    public void resendCode(String email) {

        ResendConfirmationCodeRequest request =
                ResendConfirmationCodeRequest.builder()
                        .clientId(clientId)
                        .username(email)
                        .build();

        cognitoClient.resendConfirmationCode(request);
    }
}