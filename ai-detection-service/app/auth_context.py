# app/auth_context.py
from fastapi import Header, HTTPException, Depends
from pydantic import BaseModel


class CurrentUser(BaseModel):
    actor_type: str
    user_id: str
    email: str | None = None
    role: str
    groups: list[str] = []
    scopes: list[str] = []
    request_id: str | None = None


def get_current_user(
        actor_type: str | None = Header(default=None, alias="X-Auth-Actor-Type"),
        user_id: str | None = Header(default=None, alias="X-Auth-User-Id"),
        email: str | None = Header(default=None, alias="X-Auth-User-Email"),
        role: str | None = Header(default=None, alias="X-Auth-Role"),
        groups: str | None = Header(default="", alias="X-Auth-Groups"),
        scopes: str | None = Header(default="", alias="X-Auth-Scopes"),
        request_id: str | None = Header(default=None, alias="X-Request-Id"),
):
    if not user_id:
        raise HTTPException(
            status_code=401,
            detail="Missing X-Auth-User-Id from API Gateway"
        )

    if not role:
        raise HTTPException(
            status_code=403,
            detail="Missing X-Auth-Role from API Gateway"
        )

    return CurrentUser(
        actor_type=actor_type or "USER",
        user_id=user_id,
        email=email,
        role=role,
        groups=[g for g in groups.split(",") if g],
        scopes=[s for s in scopes.split(" ") if s],
        request_id=request_id
    )


def require_role(required_role: str):
    def checker(current_user: CurrentUser = Depends(get_current_user)):
        if current_user.role != required_role:
            raise HTTPException(status_code=403, detail="Forbidden")
        return current_user

    return checker