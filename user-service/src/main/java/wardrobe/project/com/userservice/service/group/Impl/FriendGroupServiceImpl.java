package wardrobe.project.com.userservice.service.group.Impl;

import com.wardrobe.common.auth.AuthContext;
import com.wardrobe.common.auth.AuthContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wardrobe.project.com.userservice.dto.request.group.CreateFriendGroupRequest;
import wardrobe.project.com.userservice.dto.response.group.FriendGroupResponse;
import wardrobe.project.com.userservice.entity.FriendGroup;
import wardrobe.project.com.userservice.entity.FriendGroupMember;
import wardrobe.project.com.userservice.entity.User;
import wardrobe.project.com.userservice.enums.FriendGroupRole;
import wardrobe.project.com.userservice.mapper.FriendGroupMapper;
import wardrobe.project.com.userservice.repository.FriendGroupMemberRepository;
import wardrobe.project.com.userservice.repository.FriendGroupRepository;
import wardrobe.project.com.userservice.repository.UserRepository;
import wardrobe.project.com.userservice.service.group.FriendGroupService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendGroupServiceImpl implements FriendGroupService {
    private final FriendGroupRepository friendGroupRepository;
    private final FriendGroupMemberRepository friendGroupMemberRepository;
    private final UserRepository userRepository;
    private final FriendGroupMapper friendGroupMapper;
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
                .active(true)
                .build();

        FriendGroup savedGroup = friendGroupRepository.save(group);

        FriendGroupMember ownerMember = FriendGroupMember.builder()
                .group(savedGroup)
                .user(owner)
                .role(FriendGroupRole.OWNER)
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

        return friendGroupRepository.findByActiveTrueOrderByCreatedAtDesc()
                .stream()
                .filter(group -> !friendGroupMemberRepository.existsByGroupAndUserAndActiveTrue(group, user))
                .map(group -> toResponse(group, null))
                .toList();
    }

    @Transactional
    public FriendGroupResponse joinGroup(String groupId) {
        AuthContext authContext = AuthContextHolder.get();
        String email = authContext.requireEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        FriendGroup group = friendGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhóm"));

        if (Boolean.FALSE.equals(group.getActive())) {
            throw new RuntimeException("Nhóm này đã bị khóa");
        }

        boolean alreadyJoined =
                friendGroupMemberRepository.existsByGroupAndUserAndActiveTrue(group, user);

        if (alreadyJoined) {
            throw new RuntimeException("Bạn đã tham gia nhóm này rồi");
        }

        FriendGroupMember member = FriendGroupMember.builder()
                .group(group)
                .user(user)
                .role(FriendGroupRole.MEMBER)
                .active(true)
                .build();

        friendGroupMemberRepository.save(member);

        return toResponse(group, FriendGroupRole.MEMBER);
    }

    private FriendGroupResponse toResponse(FriendGroup group, FriendGroupRole myRole) {
        long memberCount = friendGroupMemberRepository.countByGroupAndActiveTrue(group);

        return friendGroupMapper.toResponse(group, myRole, memberCount);
    }
}
