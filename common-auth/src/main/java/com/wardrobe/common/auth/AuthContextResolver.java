package com.wardrobe.common.auth;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AuthContextResolver {

    public AuthContext resolve(HttpServletRequest request) {
        String actorTypeValue = getHeader(request, AuthHeaderNames.ACTOR_TYPE);

        if (isBlank(actorTypeValue)) {
            throw new AuthException(
                    AuthErrorCode.AUTH_CONTEXT_MISSING,
                    "Missing header: " + AuthHeaderNames.ACTOR_TYPE
            );
        }

        ActorType actorType = parseActorType(actorTypeValue);

        String userId = getHeader(request, AuthHeaderNames.USER_ID);
        Role role = parseRoleNullable(getHeader(request, AuthHeaderNames.ROLE));
        List<String> groups = parseCsv(getHeader(request, AuthHeaderNames.GROUPS));
        List<String> scopes = parseScopes(getHeader(request, AuthHeaderNames.SCOPES));
        String requestId = getHeader(request, AuthHeaderNames.REQUEST_ID);

        validateActor(actorType, userId);

        return new AuthContext(
                actorType,
                userId,
                role,
                groups,
                scopes,
                requestId
        );
    }

    private void validateActor(ActorType actorType, String userId) {
        if (actorType == ActorType.USER && isBlank(userId)) {
            throw new AuthException(
                    AuthErrorCode.AUTH_CONTEXT_INVALID,
                    "Missing user id for USER actor"
            );
        }
    }

    private String getHeader(HttpServletRequest request, String name) {
        return request.getHeader(name);
    }

    private ActorType parseActorType(String value) {
        try {
            return ActorType.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new AuthException(
                    AuthErrorCode.AUTH_CONTEXT_INVALID,
                    "Invalid actor type: " + value
            );
        }
    }

    private Role parseRoleNullable(String value) {
        if (isBlank(value)) {
            return null;
        }

        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new AuthException(
                    AuthErrorCode.AUTH_CONTEXT_INVALID,
                    "Invalid role: " + value
            );
        }
    }

    private List<String> parseCsv(String value) {
        if (isBlank(value)) {
            return Collections.emptyList();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private List<String> parseScopes(String value) {
        if (isBlank(value)) {
            return Collections.emptyList();
        }

        return Arrays.stream(value.split("\\s+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}