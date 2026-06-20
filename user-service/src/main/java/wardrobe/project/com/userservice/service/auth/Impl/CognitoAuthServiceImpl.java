package wardrobe.project.com.userservice.service.auth.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;
import wardrobe.project.com.userservice.config.CognitoProperties;
import wardrobe.project.com.userservice.dto.request.auth.RegisterRequest;
import wardrobe.project.com.userservice.dto.response.auth.CognitoLoginResponse;
import wardrobe.project.com.userservice.dto.response.user.UserResponse;
import wardrobe.project.com.userservice.entity.User;
import wardrobe.project.com.userservice.enums.Role;
import wardrobe.project.com.userservice.enums.UserStatus;
import wardrobe.project.com.userservice.mapper.UserMapper;
import wardrobe.project.com.userservice.repository.UserRepository;
import wardrobe.project.com.userservice.service.auth.CognitoAuthService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CognitoAuthServiceImpl implements CognitoAuthService {

    private final CognitoIdentityProviderClient cognitoClient;
    private final CognitoProperties cognitoProperties;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

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
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String cognitoSub = registerAndAddDefaultGroup(
                request.getEmail(),
                request.getPassword()
        );

        User user = User.builder()
                .userId(cognitoSub)
                .email(request.getEmail())
                .status(UserStatus.ACTIVE)
                .role(Role.ROLE_USER)
                .build();

        User savedUser = userRepository.save(user);

        return userMapper.toUserResponse(savedUser);
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

    @Override
    public String registerAndAddDefaultGroup(String email, String password) {
        SignUpResponse signUpResponse = cognitoClient.signUp(SignUpRequest.builder()
                .clientId(cognitoProperties.getClientId())
                .username(email)
                .password(password)
                .userAttributes(
                        AttributeType.builder()
                                .name("email")
                                .value(email)
                                .build()
                )
                .build());

        String cognitoSub = signUpResponse.userSub();

        cognitoClient.adminAddUserToGroup(AdminAddUserToGroupRequest.builder()
                .userPoolId(cognitoProperties.getUserPoolId())
                .username(email)
                .groupName(cognitoProperties.getDefaultGroup())
                .build());

        return cognitoSub;
    }

    @Override
    public CognitoLoginResponse refresh(String refreshToken) {
        InitiateAuthRequest request = InitiateAuthRequest.builder()
                .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                .clientId(clientId)
                .authParameters(Map.of(
                        "REFRESH_TOKEN", refreshToken
                ))
                .build();

        var response = cognitoClient.initiateAuth(request);
        var result = response.authenticationResult();

        return new CognitoLoginResponse(
                result.accessToken(),
                result.idToken(),
                refreshToken
        );
    }
}