package wardrobe.project.com.userservice.service.admin.Impl;

import com.wardrobe.common.auth.AuthContext;
import com.wardrobe.common.auth.AuthContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import wardrobe.project.com.userservice.dto.PageResponse;
import wardrobe.project.com.userservice.dto.request.admin.UpdateUserAdminRequest;
import wardrobe.project.com.userservice.dto.response.admin.UserManagementResponse;
import wardrobe.project.com.userservice.entity.User;
import wardrobe.project.com.userservice.enums.Role;
import wardrobe.project.com.userservice.enums.UserStatus;
import wardrobe.project.com.userservice.event.UserStatusChangedEvent;
import wardrobe.project.com.userservice.exception.AppException;
import wardrobe.project.com.userservice.exception.ErrorCode;
import wardrobe.project.com.userservice.kafka.UserStatusEventProducer;
import wardrobe.project.com.userservice.repository.UserRepository;
import wardrobe.project.com.userservice.service.redis.BlockedUserCacheService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class UserManagementServiceImpl implements wardrobe.project.com.userservice.service.admin.UserManagementService {
    private final UserRepository userRepository;
    private final BlockedUserCacheService blockedUserCacheService;
    private final UserStatusEventProducer userStatusEventProducer;


    @Override
    public PageResponse<UserManagementResponse> getUsersForAdmin(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);

        List<UserManagementResponse> userResponses = userPage.getContent().stream()
                .map(user -> UserManagementResponse.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .address(user.getAddress())
                        .status(user.getStatus().name())
                        .phoneNumber(user.getPhoneNumber())
                        .fullName(user.getFullName())
                        .avatarUrl(user.getAvatarUrl())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .createdAt(String.valueOf(user.getCreatedAt()))
                        .build())
                .toList();

        return PageResponse.<UserManagementResponse>builder()
                .items(userResponses)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalItems(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .hasNext(userPage.hasNext())
                .hasPrevious(userPage.hasPrevious())
                .build();
    }

    @Override
    @Transactional
    public UserManagementResponse updateUser(
            UpdateUserAdminRequest request,
            String userId
    ) {
        AuthContext authContext = AuthContextHolder.get();
        String userIdFromToken = authContext.getUserId();

        if (userIdFromToken.equals(userId)) {
            throw new AppException(
                    ErrorCode.CANNOT_UPDATE_YOURSELF
            );
        }

        // Backend chặn tuyệt đối việc sửa role
        if (request.getRole() != null) {
            throw new AppException(
                    ErrorCode.ROLE_UPDATE_NOT_ALLOWED
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new AppException(ErrorCode.USER_NOT_FOUND)
                );

        UserStatus oldStatus = user.getStatus();

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (request.getAddress() != null) {
            user.setAddress(request.getAddress().trim());
        }

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername().trim());
        }

        if (request.getStatus() != null
                && !request.getStatus().isBlank()) {
            try {
                UserStatus newStatus = UserStatus.valueOf(
                        request.getStatus()
                                .trim()
                                .toUpperCase()
                );

                user.setStatus(newStatus);

            } catch (IllegalArgumentException exception) {
                throw new AppException(
                        ErrorCode.INVALID_USER_STATUS
                );
            }
        }

        User savedUser = userRepository.save(user);

        UserStatus newStatus = savedUser.getStatus();

        // Chỉ cập nhật Redis khi status thực sự thay đổi
        if (oldStatus != newStatus) {
            updateRedisAfterCommit(savedUser, newStatus);

            publishStatusEventAfterCommit(
                    savedUser,
                    oldStatus,
                    newStatus,
                    authContext.getUserId()
            );
        }

        return UserManagementResponse.builder()
                .userId(savedUser.getUserId())
                .username(savedUser.getUsername())
                .fullName(savedUser.getFullName())
                .avatarUrl(savedUser.getAvatarUrl())
                .phoneNumber(savedUser.getPhoneNumber())
                .address(savedUser.getAddress())
                .status(savedUser.getStatus().name())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .createdAt(String.valueOf(savedUser.getCreatedAt()))
                .build();
    }

    private void publishStatusEventAfterCommit(
            User user,
            UserStatus oldStatus,
            UserStatus newStatus,
            String changedBy
    ) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        UserStatusChangedEvent event =
                                new UserStatusChangedEvent(
                                        UUID.randomUUID(),
                                        user.getUserId(),
                                        oldStatus.name(),
                                        newStatus.name(),
                                        changedBy,
                                        Instant.now()
                                );

                        userStatusEventProducer.publish(event);
                    }
                }
        );
    }

    private void updateRedisAfterCommit(
            User user,
            UserStatus newStatus
    ) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        String redisUserId = user.getUserId();

                        if (newStatus == UserStatus.INACTIVE
                                || newStatus == UserStatus.BLOCKED) {

                            blockedUserCacheService.block(redisUserId);

                        } else if (newStatus == UserStatus.ACTIVE) {

                            blockedUserCacheService.unblock(redisUserId);
                        }
                    }
                }
        );
    }
}
