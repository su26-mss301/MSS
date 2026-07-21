package wardrobe.project.com.userservice.service.group.Impl;

import com.wardrobe.common.auth.AuthContext;
import com.wardrobe.common.auth.AuthContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.userservice.dto.request.group.CreateFriendGroupJoinRequest;
import wardrobe.project.com.userservice.dto.request.group.CreateFriendGroupRequest;
import wardrobe.project.com.userservice.dto.request.group.InviteFriendGroupMemberRequest;
import wardrobe.project.com.userservice.dto.request.group.UpdateFriendGroupRequest;
import wardrobe.project.com.userservice.dto.response.group.*;
import wardrobe.project.com.userservice.entity.*;
import wardrobe.project.com.userservice.enums.FriendGroupInvitationStatus;
import wardrobe.project.com.userservice.enums.FriendGroupJoinRequestStatus;
import wardrobe.project.com.userservice.enums.FriendGroupMemberStatus;
import wardrobe.project.com.userservice.enums.FriendGroupRole;
import wardrobe.project.com.userservice.mapper.FriendGroupMapper;
import wardrobe.project.com.userservice.mapper.UserStylePreferenceMapper;
import wardrobe.project.com.userservice.repository.*;
import wardrobe.project.com.userservice.service.group.FriendGroupService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FriendGroupServiceImpl implements FriendGroupService {
    private final FriendGroupRepository friendGroupRepository;
    private final FriendGroupMemberRepository friendGroupMemberRepository;
    private final UserRepository userRepository;
    private final FriendGroupMapper friendGroupMapper;
    private final UserStylePreferenceRepository stylePreferenceRepository;
    private final UserStylePreferenceMapper stylePreferenceMapper;
    private final FriendGroupInvitationRepository friendGroupInvitationRepository;
    private final FriendGroupJoinRequestRepository friendGroupJoinRequestRepository;
    @Transactional
    public FriendGroupResponse createGroup(CreateFriendGroupRequest request) {
        AuthContext authContext = AuthContextHolder.get();
        String email = authContext.requireEmail();

        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        FriendGroup group = FriendGroup.builder()
                .owner(owner)
                .groupName(request.getGroupName().trim())
                .description(request.getDescription())
                .emoji(
                        request.getEmoji() == null || request.getEmoji().isBlank()
                                ? "👗"
                                : request.getEmoji()
                )
                .primaryStyle(
                        request.getPrimaryStyles() == null || request.getPrimaryStyles().isEmpty()
                                ? null
                                : stylePreferenceMapper.toJson(request.getPrimaryStyles())
                )
                .active(true)
                .build();

        FriendGroup savedGroup = friendGroupRepository.save(group);

        FriendGroupMember ownerMember = FriendGroupMember.builder()
                .group(savedGroup)
                .user(owner)
                .role(FriendGroupRole.OWNER)
                .status(FriendGroupMemberStatus.ACTIVE)
                .active(true)
                .build();

        friendGroupMemberRepository.save(ownerMember);

        return toResponse(savedGroup, FriendGroupRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<FriendGroupResponse> getMyGroups() {
        AuthContext authContext = AuthContextHolder.get();
        String email = authContext.requireEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        List<FriendGroupMember> memberships =
                friendGroupMemberRepository.findByUserAndActiveTrueOrderByCreatedAtDesc(user);

        return memberships.stream()
                .map(member -> toResponse(member.getGroup(), member.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendGroupResponse> discoverGroups() {
        AuthContext authContext = AuthContextHolder.get();
        String email = authContext.requireEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Lấy sở thích của user
        List<String> myStyles = stylePreferenceRepository.findByUser(user)
                .map(pref -> stylePreferenceMapper.fromJson(pref.getPreferredStyles()))
                .orElse(List.of());

        return friendGroupRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .filter(group -> !friendGroupMemberRepository.existsByGroupAndUserAndActiveTrue(group, user))
                // Nhóm chưa có style hoặc có style thuộc sở thích của user
                .filter(group -> {
                    if (group.getPrimaryStyle() == null) return true;
                    List<String> groupStyles = stylePreferenceMapper.fromJson(group.getPrimaryStyle());
                    return groupStyles.stream().anyMatch(myStyles::contains);
                })
                .map(group -> toResponse(group, null))
                .toList();
    }

    @Override
    @Transactional
    public void requestToJoinGroup(String groupId, CreateFriendGroupJoinRequest request) {
        User currentUser = getCurrentUser();
        FriendGroup group = getActiveGroup(groupId);

        Optional<FriendGroupMember> existingMemberOpt =
                friendGroupMemberRepository.findByGroupAndUser(group, currentUser);

        boolean previouslyKicked = false;

        if (existingMemberOpt.isPresent()) {
            FriendGroupMember existingMember = existingMemberOpt.get();

            if (existingMember.getActive()) {
                throw new RuntimeException("Bạn đã là thành viên của nhóm này");
            }

            if (existingMember.getStatus() == FriendGroupMemberStatus.KICKED) {
                previouslyKicked = true;
            }
        }

        boolean hasPendingRequest = friendGroupJoinRequestRepository
                .existsByGroupAndRequesterAndStatus(
                        group,
                        currentUser,
                        FriendGroupJoinRequestStatus.PENDING
                );

        if (hasPendingRequest) {
            throw new RuntimeException("Bạn đã gửi yêu cầu tham gia nhóm này rồi");
        }

        FriendGroupJoinRequest joinRequest = FriendGroupJoinRequest.builder()
                .group(group)
                .requester(currentUser)
                .message(request.getMessage())
                .status(FriendGroupJoinRequestStatus.PENDING)
                .expiredAt(Instant.now().plusSeconds(7 * 24 * 60 * 60)) // 7 days
                .previouslyKicked(previouslyKicked)
                .build();

        friendGroupJoinRequestRepository.save(joinRequest);
    }

    @Override
    public FriendGroupDetailResponse getGroupDetail(String groupId) {
        AuthContext authContext = AuthContextHolder.get();
        String email = authContext.requireEmail();

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        FriendGroup group = friendGroupRepository.findByGroupIdAndActiveTrue(groupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm"));

        FriendGroupMember myMember = friendGroupMemberRepository
                .findByGroupAndUserAndActiveTrue(group, currentUser)
                .orElseThrow(() -> new RuntimeException("Bạn chưa tham gia nhóm này"));

        List<FriendGroupMember> members =
                friendGroupMemberRepository.findByGroupAndActiveTrue(group);

        List<GroupStyleStatResponse> commonStyles = buildCommonStyles(members);
        List<String> colorPalette = buildColorPalette(members);

        List<String> primaryStyles = stylePreferenceMapper.fromJson(group.getPrimaryStyle());
        List<String> primaryStyleLabels = primaryStyles.stream()
                .map(this::toStyleLabel)
                .toList();

        List<GroupActiveMemberResponse> activeMembers = members.stream()
                .limit(5)
                .map(this::toActiveMemberResponse)
                .toList();

        List<GroupMemberResponse> memberResponses = members.stream()
                .map(this::toGroupMemberResponse)
                .toList();

        return FriendGroupDetailResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .description(group.getDescription())
                .emoji(group.getEmoji())
                .myRole(myMember.getRole().name())
                .memberCount(members.size())
                .primaryStyles(primaryStyles)
                .primaryStyleLabels(primaryStyleLabels)
                .status(Boolean.TRUE.equals(group.getActive()) ? "Hoạt Động" : "Không Hoạt Động")
                .createdAt(group.getCreatedAt())
                .commonStyles(commonStyles)
                .colorPalette(colorPalette)
                .activeMembers(activeMembers)
                .members(memberResponses)
                .build();
    }

    @Override
    @Transactional
    public void inviteMember(String groupId, InviteFriendGroupMemberRequest request) {
        User currentUser = getCurrentUser();
        FriendGroup group = getActiveGroup(groupId);

        ensureOwner(group, currentUser);

        String email = request.getEmail() == null
                ? ""
                : request.getEmail().trim().toLowerCase();

        if (email.isBlank()) {
            throw new RuntimeException("Email is required");
        }

        User invitee = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (invitee.getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("You cannot invite yourself");
        }

        boolean alreadyMember = friendGroupMemberRepository
                .findByGroupAndUserAndActiveTrue(group, invitee)
                .isPresent();

        if (alreadyMember) {
            throw new RuntimeException("User is already a member of this group");
        }

        boolean hasPendingInvitation = friendGroupInvitationRepository
                .existsByGroupAndInviteeAndStatus(
                        group,
                        invitee,
                        FriendGroupInvitationStatus.PENDING
                );

        if (hasPendingInvitation) {
            throw new RuntimeException("User already has a pending invitation");
        }

        FriendGroupInvitation invitation = FriendGroupInvitation.builder()
                .group(group)
                .inviter(currentUser)
                .invitee(invitee)
                .status(FriendGroupInvitationStatus.PENDING)
                .expiredAt(Instant.now().plusSeconds(7 * 24 * 60 * 60)) // 7 days
                .build();

        friendGroupInvitationRepository.save(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendGroupInvitationResponse> getMyPendingInvitations() {
        User currentUser = getCurrentUser();

        return friendGroupInvitationRepository
                .findAllByInviteeAndStatusOrderByCreatedAtDesc(
                        currentUser,
                        FriendGroupInvitationStatus.PENDING
                )
                .stream()
                .filter(invitation -> invitation.getExpiredAt().isAfter(Instant.now()))
                .map(this::toInvitationResponse)
                .toList();
    }

    @Override
    @Transactional
    public void acceptInvitation(String invitationId) {
        User currentUser = getCurrentUser();

        FriendGroupInvitation invitation = friendGroupInvitationRepository
                .findByInvitationIdAndInviteeAndStatus(
                        invitationId,
                        currentUser,
                        FriendGroupInvitationStatus.PENDING
                )
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (invitation.getExpiredAt().isBefore(Instant.now())) {
            invitation.setStatus(FriendGroupInvitationStatus.EXPIRED);
            friendGroupInvitationRepository.save(invitation);
            throw new RuntimeException("Invitation expired");
        }

        FriendGroup group = invitation.getGroup();

        if (!group.getActive()) {
            throw new RuntimeException("Friend group is no longer active");
        }

        Optional<FriendGroupMember> existingMember =
                friendGroupMemberRepository.findByGroupAndUser(group, currentUser);

        if (existingMember.isPresent()) {
            FriendGroupMember member = existingMember.get();

            if (member.getActive()) {
                invitation.setStatus(FriendGroupInvitationStatus.ACCEPTED);
                invitation.setRespondedAt(Instant.now());
                friendGroupInvitationRepository.save(invitation);
                return;
            }

            member.setActive(true);
            member.setStatus(FriendGroupMemberStatus.ACTIVE);
            member.setRole(FriendGroupRole.MEMBER);
            friendGroupMemberRepository.save(member);
        } else {
            FriendGroupMember member = FriendGroupMember.builder()
                    .group(group)
                    .user(currentUser)
                    .role(FriendGroupRole.MEMBER)
                    .status(FriendGroupMemberStatus.ACTIVE)
                    .active(true)
                    .build();

            friendGroupMemberRepository.save(member);
        }

        invitation.setStatus(FriendGroupInvitationStatus.ACCEPTED);
        invitation.setRespondedAt(Instant.now());

        friendGroupInvitationRepository.save(invitation);
    }

    @Override
    @Transactional
    public void declineInvitation(String invitationId) {
        User currentUser = getCurrentUser();

        FriendGroupInvitation invitation = friendGroupInvitationRepository
                .findByInvitationIdAndInviteeAndStatus(
                        invitationId,
                        currentUser,
                        FriendGroupInvitationStatus.PENDING
                )
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        if (invitation.getExpiredAt().isBefore(Instant.now())) {
            invitation.setStatus(FriendGroupInvitationStatus.EXPIRED);
        } else {
            invitation.setStatus(FriendGroupInvitationStatus.DECLINED);
        }

        invitation.setRespondedAt(Instant.now());

        friendGroupInvitationRepository.save(invitation);
    }

    @Override
    @Transactional
    public void cancelInvitation(String groupId, String invitationId) {
        User currentUser = getCurrentUser();
        FriendGroup group = getActiveGroup(groupId);

        ensureOwner(group, currentUser);

        FriendGroupInvitation invitation = friendGroupInvitationRepository
                .findByInvitationIdAndGroupAndStatus(
                        invitationId,
                        group,
                        FriendGroupInvitationStatus.PENDING
                )
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        invitation.setStatus(FriendGroupInvitationStatus.CANCELLED);
        invitation.setRespondedAt(Instant.now());

        friendGroupInvitationRepository.save(invitation);
    }

    @Override
    @Transactional
    public void removeMember(String groupId, String memberId) {
        User currentUser = getCurrentUser();
        FriendGroup group = getActiveGroup(groupId);

        ensureOwner(group, currentUser);

        FriendGroupMember targetMember = friendGroupMemberRepository
                .findByMemberIdAndGroupAndActiveTrue(memberId, group)
                .orElseThrow(() -> new RuntimeException("Member not found in this group"));

        if (targetMember.getRole() == FriendGroupRole.OWNER) {
            throw new RuntimeException("Cannot remove group owner");
        }

        if (targetMember.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Owner cannot remove yourself");
        }

        targetMember.setActive(false);
        targetMember.setStatus(FriendGroupMemberStatus.KICKED);

        friendGroupMemberRepository.save(targetMember);
    }

    @Override
    @Transactional
    public void deleteGroup(String groupId) {
        User currentUser = getCurrentUser();
        FriendGroup group = getActiveGroup(groupId);

        ensureOwner(group, currentUser);

        group.setActive(false);
        friendGroupRepository.save(group);

        List<FriendGroupMember> members =
                friendGroupMemberRepository.findAllByGroupAndActiveTrue(group);

        for (FriendGroupMember member : members) {
            member.setActive(false);
        }

        friendGroupMemberRepository.saveAll(members);

        List<FriendGroupInvitation> pendingInvitations =
                friendGroupInvitationRepository.findAllByGroupAndStatusOrderByCreatedAtDesc(
                        group,
                        FriendGroupInvitationStatus.PENDING
                );

        for (FriendGroupInvitation invitation : pendingInvitations) {
            invitation.setStatus(FriendGroupInvitationStatus.CANCELLED);
            invitation.setRespondedAt(Instant.now());
        }

        friendGroupInvitationRepository.saveAll(pendingInvitations);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FriendGroupJoinRequestResponse> getGroupJoinRequests(String groupId) {
        User currentUser = getCurrentUser();
        FriendGroup group = getActiveGroup(groupId);

        ensureOwner(group, currentUser);

        return friendGroupJoinRequestRepository
                .findAllByGroupAndStatusOrderByCreatedAtDesc(
                        group,
                        FriendGroupJoinRequestStatus.PENDING
                )
                .stream()
                .filter(req -> req.getExpiredAt().isAfter(Instant.now()))
                .map(this::toJoinRequestResponse)
                .toList();
    }

    @Override
    @Transactional
    public void acceptJoinRequest(String requestId) {
        User currentUser = getCurrentUser();

        FriendGroupJoinRequest joinRequest = friendGroupJoinRequestRepository
                .findByRequestIdAndStatus(requestId, FriendGroupJoinRequestStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("Yêu cầu tham gia không tồn tại"));

        FriendGroup group = joinRequest.getGroup();

        ensureOwner(group, currentUser);

        if (joinRequest.getExpiredAt().isBefore(Instant.now())) {
            joinRequest.setStatus(FriendGroupJoinRequestStatus.EXPIRED);
            friendGroupJoinRequestRepository.save(joinRequest);
            throw new RuntimeException("Yêu cầu tham gia đã hết hạn");
        }

        User requester = joinRequest.getRequester();

        Optional<FriendGroupMember> existingMemberOpt =
                friendGroupMemberRepository.findByGroupAndUser(group, requester);

        if (existingMemberOpt.isPresent()) {
            FriendGroupMember member = existingMemberOpt.get();

            member.setActive(true);
            member.setStatus(FriendGroupMemberStatus.ACTIVE);
            member.setRole(FriendGroupRole.MEMBER);

            friendGroupMemberRepository.save(member);
        } else {
            FriendGroupMember member = FriendGroupMember.builder()
                    .group(group)
                    .user(requester)
                    .role(FriendGroupRole.MEMBER)
                    .active(true)
                    .status(FriendGroupMemberStatus.ACTIVE)
                    .build();

            friendGroupMemberRepository.save(member);
        }

        joinRequest.setStatus(FriendGroupJoinRequestStatus.ACCEPTED);
        joinRequest.setRespondedAt(Instant.now());
        joinRequest.setRespondedBy(currentUser);

        friendGroupJoinRequestRepository.save(joinRequest);
    }

    @Override
    @Transactional
    public void rejectJoinRequest(String requestId) {
        User currentUser = getCurrentUser();

        FriendGroupJoinRequest joinRequest = friendGroupJoinRequestRepository
                .findByRequestIdAndStatus(requestId, FriendGroupJoinRequestStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("Yêu cầu tham gia không tồn tại"));

        FriendGroup group = joinRequest.getGroup();

        ensureOwner(group, currentUser);

        if (joinRequest.getExpiredAt().isBefore(Instant.now())) {
            joinRequest.setStatus(FriendGroupJoinRequestStatus.EXPIRED);
        } else {
            joinRequest.setStatus(FriendGroupJoinRequestStatus.REJECTED);
        }

        joinRequest.setRespondedAt(Instant.now());
        joinRequest.setRespondedBy(currentUser);

        friendGroupJoinRequestRepository.save(joinRequest);
    }

    @Override
    @Transactional
    public void cancelMyJoinRequest(String requestId) {
        User currentUser = getCurrentUser();

        FriendGroupJoinRequest joinRequest = friendGroupJoinRequestRepository
                .findByRequestIdAndStatus(requestId, FriendGroupJoinRequestStatus.PENDING)
                .orElseThrow(() -> new RuntimeException("Yêu cầu tham gia không tồn tại"));

        if (!joinRequest.getRequester().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Bạn không có quyền hủy yêu cầu này");
        }

        joinRequest.setStatus(FriendGroupJoinRequestStatus.CANCELLED);
        joinRequest.setRespondedAt(Instant.now());

        friendGroupJoinRequestRepository.save(joinRequest);
    }

    @Override
    @Transactional
    public void leaveGroup(String groupId) {
        User currentUser = getCurrentUser();
        FriendGroup group = getActiveGroup(groupId);

        FriendGroupMember member = friendGroupMemberRepository
                .findByGroupAndUserAndActiveTrue(group, currentUser)
                .orElseThrow(() -> new RuntimeException("Bạn chưa tham gia nhóm này"));

        if (member.getRole() == FriendGroupRole.OWNER) {
            throw new RuntimeException(
                    "Chủ nhóm không thể tự rời nhóm. Vui lòng chuyển quyền chủ nhóm hoặc giải tán nhóm."
            );
        }

        member.setActive(false);
        member.setStatus(FriendGroupMemberStatus.LEFT);

        friendGroupMemberRepository.save(member);
    }

    @Override
    @Transactional
    public FriendGroupResponse updateGroup(String groupId, UpdateFriendGroupRequest request) {
        User currentUser = getCurrentUser();
        FriendGroup group = getActiveGroup(groupId);

        ensureOwner(group, currentUser);

        if (request.getGroupName() != null && !request.getGroupName().trim().isBlank()) {
            group.setGroupName(request.getGroupName().trim());
        }

        if (request.getDescription() != null) {
            group.setDescription(request.getDescription().trim());
        }

        if (request.getEmoji() != null && !request.getEmoji().trim().isBlank()) {
            group.setEmoji(request.getEmoji().trim());
        }

        FriendGroup savedGroup = friendGroupRepository.save(group);

        return toResponse(savedGroup, FriendGroupRole.OWNER);
    }

    private FriendGroupResponse toResponse(FriendGroup group, FriendGroupRole myRole) {
        long memberCount = friendGroupMemberRepository.countByGroupAndActiveTrue(group);
        List<FriendGroupMember> members =
                friendGroupMemberRepository.findByGroupAndActiveTrue(group);
        List<String> colorPalette = buildColorPalette(members);

        return friendGroupMapper.toResponse(group, myRole, memberCount, colorPalette);
    }

    private List<GroupStyleStatResponse> buildCommonStyles(List<FriendGroupMember> members) {
        Map<String, Long> styleCount = new LinkedHashMap<>();

        for (FriendGroupMember member : members) {
            stylePreferenceRepository.findByUser(member.getUser())
                    .ifPresent(preference -> {
                        List<String> styles = stylePreferenceMapper.fromJson(
                                preference.getPreferredStyles()
                        );

                        for (String style : styles) {
                            styleCount.put(style, styleCount.getOrDefault(style, 0L) + 1);
                        }
                    });
        }

        int memberCount = members.size();

        return styleCount.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> {
                    int percentage = memberCount == 0
                            ? 0
                            : (int) Math.round((entry.getValue() * 100.0) / memberCount);

                    return GroupStyleStatResponse.builder()
                            .styleName(entry.getKey())
                            .label(toStyleLabel(entry.getKey()))
                            .percentage(percentage)
                            .build();
                })
                .toList();
    }

    private List<String> buildColorPalette(List<FriendGroupMember> members) {
        Map<String, Long> colorCount = new LinkedHashMap<>();

        for (FriendGroupMember member : members) {
            stylePreferenceRepository.findByUser(member.getUser())
                    .ifPresent(preference -> {
                        List<String> colors = stylePreferenceMapper.fromJson(
                                preference.getFavoriteColors()
                        );

                        for (String color : colors) {
                            colorCount.put(color, colorCount.getOrDefault(color, 0L) + 1);
                        }
                    });
        }

        return colorCount.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .map(entry -> toColorHex(entry.getKey()))
                .toList();
    }

    private GroupActiveMemberResponse toActiveMemberResponse(FriendGroupMember member) {
        User user = member.getUser();

        String mainStyle = stylePreferenceRepository.findByUser(user)
                .map(preference -> {
                    List<String> styles = stylePreferenceMapper.fromJson(
                            preference.getPreferredStyles()
                    );

                    return styles.isEmpty() ? null : styles.get(0);
                })
                .orElse(null);

        return GroupActiveMemberResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .mainStyle(mainStyle)
                .mainStyleLabel(toStyleLabel(mainStyle))
                .role(member.getRole().name())
                .build();
    }

    private GroupMemberResponse toGroupMemberResponse(FriendGroupMember member) {
        User user = member.getUser();

        return GroupMemberResponse.builder()
                .memberId(member.getMemberId())
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(member.getRole().name())
                .joinedAt(member.getCreatedAt())
                .build();
    }

    private String toStyleLabel(String style) {
        if (style == null) return null;

        return switch (style) {
            case "MINIMAL" -> "Tối Giản";
            case "CASUAL" -> "Thường Ngày";
            case "OFFICE" -> "Công Sở";
            case "ELEGANT" -> "Trang Trọng";
            case "STREET" -> "Đường Phố";
            case "BOHEMIAN" -> "Bohemian";
            case "SPORTY" -> "Thể Thao";
            case "VINTAGE" -> "Cổ Điển";
            default -> style;
        };
    }

    private String toColorHex(String color) {
        if (color == null) return "#94A3B8";

        return switch (color) {
            case "BLACK" -> "#000000";
            case "WHITE" -> "#FFFFFF";
            case "NAVY" -> "#1E3A5F";
            case "CREAM" -> "#F5E6C8";
            case "PURPLE" -> "#8B5CF6";
            case "PINK" -> "#EC4899";
            case "RED" -> "#EF4444";
            case "ORANGE" -> "#F97316";
            case "YELLOW" -> "#F59E0B";
            case "GREEN" -> "#10B981";
            case "TURQUOISE" -> "#14B8A6";
            case "GRAY" -> "#94A3B8";
            case "BROWN" -> "#92400E";
            case "BEIGE" -> "#D4B896";
            default -> "#94A3B8";
        };
    }

    private User getCurrentUser() {
        AuthContext authContext = AuthContextHolder.get();
        String userId = authContext.requireUserId();

        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    private FriendGroup getActiveGroup(String groupId) {
        return friendGroupRepository.findById(groupId)
                .filter(FriendGroup::getActive)
                .orElseThrow(() -> new RuntimeException("Friend group not found"));
    }

    private void ensureOwner(FriendGroup group, User currentUser) {
        FriendGroupMember member = friendGroupMemberRepository
                .findByGroupAndUserAndActiveTrue(group, currentUser)
                .orElseThrow(() -> new RuntimeException("You are not a member of this group"));

        if (member.getRole() != FriendGroupRole.OWNER) {
            throw new RuntimeException("Only group owner can perform this action");
        }
    }

    private FriendGroupJoinRequestResponse toJoinRequestResponse(
            FriendGroupJoinRequest request
    ) {
        User requester = request.getRequester();
        FriendGroup group = request.getGroup();

        return FriendGroupJoinRequestResponse.builder()
                .requestId(request.getRequestId())
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .groupEmoji(group.getEmoji())
                .requesterId(requester.getUserId())
                .requesterName(requester.getFullName())
                .requesterEmail(requester.getEmail())
                .requesterAvatarUrl(requester.getAvatarUrl())
                .message(request.getMessage())
                .previouslyKicked(request.isPreviouslyKicked())
                .status(request.getStatus())
                .expiredAt(request.getExpiredAt())
                .createdAt(request.getCreatedAt())
                .build();
    }

    private FriendGroupInvitationResponse toInvitationResponse(
            FriendGroupInvitation invitation
    ) {
        return FriendGroupInvitationResponse.builder()
                .invitationId(invitation.getInvitationId())

                .groupId(invitation.getGroup().getGroupId())
                .groupName(invitation.getGroup().getGroupName())
                .groupEmoji(invitation.getGroup().getEmoji())

                .inviterId(invitation.getInviter().getUserId())
                .inviterName(invitation.getInviter().getFullName())
                .inviterEmail(invitation.getInviter().getEmail())

                .inviteeId(invitation.getInvitee().getUserId())
                .inviteeName(invitation.getInvitee().getFullName())
                .inviteeEmail(invitation.getInvitee().getEmail())

                .status(invitation.getStatus())
                .expiredAt(invitation.getExpiredAt())
                .createdAt(invitation.getCreatedAt())
                .build();
    }

    // -------------------------------------------------------------------------
    // Style-conflict helpers (dùng cho auto-leave khi đổi style preference)
    // -------------------------------------------------------------------------

    /**
     * Trả về danh sách nhóm mà user đang tham gia (MEMBER / ADMIN, không phải OWNER)
     * có primaryStyle không nằm trong tập newStyles mới của user.
     * Chỉ tính những nhóm đã đặt primaryStyle (nullable → bỏ qua).
     */
    @Override
    @Transactional(readOnly = true)
    public List<FriendGroupResponse> getStyleConflictGroups(List<String> newStyles) {
        User currentUser = getCurrentUser();

        return friendGroupMemberRepository
                .findByUserAndActiveTrueOrderByCreatedAtDesc(currentUser)
                .stream()
                // Chỉ member / admin, không phải owner
                .filter(m -> m.getRole() != FriendGroupRole.OWNER)
                // Nhóm phải có primaryStyle
                .filter(m -> m.getGroup().getPrimaryStyle() != null)
                // primaryStyle không nằm trong newStyles
                .filter(m -> {
                    List<String> groupStyles = stylePreferenceMapper.fromJson(m.getGroup().getPrimaryStyle());
                    return groupStyles.stream().noneMatch(newStyles::contains);
                })
                .map(m -> toResponse(m.getGroup(), m.getRole()))
                .toList();
    }

    /**
     * Tự động kick user ra khỏi các nhóm không còn phù hợp với style mới.
     * Được gọi sau khi user lưu preferences thành công.
     */
    @Override
    @Transactional
    public void leaveGroupsWithStyleMismatch(User user, List<String> newStyles) {
        List<FriendGroupMember> membershipsToLeave = friendGroupMemberRepository
                .findByUserAndActiveTrueOrderByCreatedAtDesc(user)
                .stream()
                .filter(m -> m.getRole() != FriendGroupRole.OWNER)
                .filter(m -> m.getGroup().getPrimaryStyle() != null)
                .filter(m -> {
                    List<String> groupStyles = stylePreferenceMapper.fromJson(m.getGroup().getPrimaryStyle());
                    return groupStyles.stream().noneMatch(newStyles::contains);
                })
                .toList();

        for (FriendGroupMember member : membershipsToLeave) {
            member.setActive(false);
            member.setStatus(FriendGroupMemberStatus.LEFT);
        }

        if (!membershipsToLeave.isEmpty()) {
            friendGroupMemberRepository.saveAll(membershipsToLeave);
        }
    }
}
