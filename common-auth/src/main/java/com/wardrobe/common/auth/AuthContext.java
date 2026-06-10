package com.wardrobe.common.auth;

import java.util.Collections;
import java.util.List;

public class AuthContext {

    private final ActorType actorType;
    private final String userId;
    private final String email;
    private final Role role;
    private final List<String> groups;
    private final List<String> scopes;
    private final String requestId;

    public AuthContext(
            ActorType actorType,
            String userId,
            String email,
            Role role,
            List<String> groups,
            List<String> scopes,
            String requestId
    ) {
        this.actorType = actorType;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.groups = groups == null ? Collections.emptyList() : List.copyOf(groups);
        this.scopes = scopes == null ? Collections.emptyList() : List.copyOf(scopes);
        this.requestId = requestId;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public Role getRole() {
        return role;
    }

    public List<String> getGroups() {
        return groups;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean isUser() {
        return actorType == ActorType.USER;
    }

    public boolean isSystem() {
        return actorType == ActorType.SYSTEM;
    }

    public boolean hasRole(Role expectedRole) {
        return role == expectedRole;
    }

    public boolean hasAnyRole(Role... expectedRoles) {
        if (expectedRoles == null || role == null) {
            return false;
        }

        for (Role expectedRole : expectedRoles) {
            if (role == expectedRole) {
                return true;
            }
        }

        return false;
    }

    public boolean hasGroup(String group) {
        return group != null && groups.contains(group);
    }

    public boolean hasScope(String scope) {
        return scope != null && scopes.contains(scope);
    }

    public void requireRole(Role expectedRole) {
        if (!hasRole(expectedRole)) {
            throw new AuthException(
                    AuthErrorCode.AUTH_FORBIDDEN_ROLE,
                    "Required role: " + expectedRole
            );
        }
    }

    public void requireAnyRole(Role... expectedRoles) {
        if (!hasAnyRole(expectedRoles)) {
            throw new AuthException(
                    AuthErrorCode.AUTH_FORBIDDEN_ROLE,
                    "Required one of roles: " + List.of(expectedRoles)
            );
        }
    }

    public void requireScope(String scope) {
        if (!hasScope(scope)) {
            throw new AuthException(
                    AuthErrorCode.AUTH_FORBIDDEN_SCOPE,
                    "Required scope: " + scope
            );
        }
    }

    public String requireUserId() {
        if (!isUser() || userId == null || userId.isBlank()) {
            throw new AuthException(
                    AuthErrorCode.AUTH_CONTEXT_INVALID,
                    "Current actor is not a valid user"
            );
        }

        return userId;
    }

    public String requireEmail() {
        if (email == null || email.isBlank()) {
            throw new AuthException(
                    AuthErrorCode.AUTH_CONTEXT_INVALID,
                    "Current user email is not available"
            );
        }

        return email;
    }
}