package com.wardrobe.common.auth;

public final class AuthHeaderNames {

    private AuthHeaderNames() {
    }

    public static final String ACTOR_TYPE = "X-Auth-Actor-Type";
    public static final String USER_ID = "X-Auth-User-Id";
    public static final String DEVICE_ID = "X-Auth-Device-Id";
    public static final String ROLE = "X-Auth-Role";
    public static final String GROUPS = "X-Auth-Groups";
    public static final String SCOPES = "X-Auth-Scopes";
    public static final String REQUEST_ID = "X-Request-Id";
}