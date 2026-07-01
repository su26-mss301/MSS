# app/auth_context.py
#
# Đọc các header do API Gateway (KeycloakAuthGlobalFilter) inject vào sau
# khi xác thực JWT Keycloak. Không tự decode JWT — gateway đã làm việc đó.
#
# Headers được inject bởi gateway:
#   X-Auth-Actor-Type : loại actor (USER / SERVICE)
#   X-Auth-User-Id    : Keycloak subject (UUID)
#   X-Auth-User-Email : email user
#   X-Auth-Username   : preferred_username
#   X-Auth-Role       : role chính (ROLE_USER | ROLE_ADMIN)
#   X-Auth-Groups     : tất cả roles, phân cách bởi dấu phẩy
#   X-Auth-Scopes     : OAuth2 scopes, phân cách bởi dấu cách
#   X-Request-Id      : UUID của request
#
from fastapi import Header, HTTPException, Depends
from pydantic import BaseModel

# Dự án chỉ có 2 role: ROLE_USER < ROLE_ADMIN
_ROLE_PRIORITY: dict[str, int] = {
    "ROLE_USER": 1,
    "ROLE_ADMIN": 2,
}


class CurrentUser(BaseModel):
    actor_type: str
    user_id: str
    email: str | None = None
    username: str | None = None
    role: str
    groups: list[str] = []
    scopes: list[str] = []
    request_id: str | None = None


def get_current_user(
        actor_type: str | None = Header(default=None, alias="X-Auth-Actor-Type"),
        user_id: str | None = Header(default=None, alias="X-Auth-User-Id"),
        email: str | None = Header(default=None, alias="X-Auth-User-Email"),
        username: str | None = Header(default=None, alias="X-Auth-Username"),
        role: str | None = Header(default=None, alias="X-Auth-Role"),
        groups: str | None = Header(default="", alias="X-Auth-Groups"),
        scopes: str | None = Header(default="", alias="X-Auth-Scopes"),
        request_id: str | None = Header(default=None, alias="X-Request-Id"),
) -> CurrentUser:
    """
    Dependency: yêu cầu user đã đăng nhập (gateway đã xác thực JWT).
    Nếu thiếu X-Auth-User-Id → 401 (request không qua gateway hoặc chưa login).
    Nếu thiếu X-Auth-Role    → 403 (gateway không resolve được role).
    """
    if not user_id:
        raise HTTPException(
            status_code=401,
            detail={
                "error": "AUTH_TOKEN_MISSING",
                "message": "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn."
            }
        )

    if not role:
        raise HTTPException(
            status_code=403,
            detail={
                "error": "AUTH_ROLE_MISSING",
                "message": "Tài khoản không có vai trò phù hợp."
            }
        )

    return CurrentUser(
        actor_type=actor_type or "USER",
        user_id=user_id,
        email=email,
        username=username,
        role=role,
        groups=[g for g in (groups or "").split(",") if g],
        scopes=[s for s in (scopes or "").split(" ") if s],
        request_id=request_id,
    )


def require_roles(*allowed_roles: str):
    """
    Dependency factory: chỉ cho phép các role trong allowed_roles.

    Hỗ trợ thứ bậc: ROLE_ADMIN tự động được phép ở mọi endpoint
    không phân biệt role yêu cầu là gì.

    Dự án có 2 role: ROLE_USER, ROLE_ADMIN

    Ví dụ:
        Depends(require_roles("ROLE_USER"))   # USER + ADMIN đều qua
        Depends(require_roles("ROLE_ADMIN"))  # chỉ ADMIN
    """
    allowed_set = set(allowed_roles)

    def _checker(current_user: CurrentUser = Depends(get_current_user)) -> CurrentUser:
        user_priority = _ROLE_PRIORITY.get(current_user.role, 0)
        min_required_priority = min(
            (_ROLE_PRIORITY.get(r, 0) for r in allowed_set),
            default=0
        )

        # Nếu độ ưu tiên của user >= độ ưu tiên tối thiểu yêu cầu → cho phép
        if user_priority >= min_required_priority and user_priority > 0:
            return current_user

        raise HTTPException(
            status_code=403,
            detail={
                "error": "AUTH_FORBIDDEN",
                "message": f"Bạn không có quyền thực hiện hành động này. "
                           f"Yêu cầu: {', '.join(allowed_roles)}. "
                           f"Vai trò hiện tại: {current_user.role}."
            }
        )

    return _checker
