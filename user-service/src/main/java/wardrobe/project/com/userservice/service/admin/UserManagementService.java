package wardrobe.project.com.userservice.service.admin;

import org.springframework.data.domain.Pageable;
import wardrobe.project.com.userservice.dto.PageResponse;
import wardrobe.project.com.userservice.dto.request.admin.UpdateUserAdminRequest;
import wardrobe.project.com.userservice.dto.response.admin.UserManagementResponse;

public interface UserManagementService {
    PageResponse<UserManagementResponse> getUsersForAdmin(Pageable pageable);
    UserManagementResponse updateUser(UpdateUserAdminRequest request, String userId);
}
