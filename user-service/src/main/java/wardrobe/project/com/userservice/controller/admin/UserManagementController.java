package wardrobe.project.com.userservice.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import wardrobe.project.com.userservice.dto.ApiResponse;
import wardrobe.project.com.userservice.dto.PageResponse;
import wardrobe.project.com.userservice.dto.request.admin.UpdateUserAdminRequest;
import wardrobe.project.com.userservice.dto.response.admin.UserManagementResponse;
import wardrobe.project.com.userservice.service.admin.UserManagementService;


@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class UserManagementController {
    private final UserManagementService userManagementService;

    @GetMapping()
    public ApiResponse<PageResponse<UserManagementResponse>> getUsersForAdmin(@PageableDefault(
            size = 10,
            sort = "createdAt",
            direction = Sort.Direction.DESC

    ) Pageable pageable) {
        return ApiResponse.success(userManagementService.getUsersForAdmin(pageable));
    }

    @PutMapping()
    public ApiResponse<UserManagementResponse> updateUser(@RequestBody UpdateUserAdminRequest request,
                                        @RequestParam("userId") String userId) {
        return ApiResponse.success(userManagementService.updateUser(request, userId));
    }
}
