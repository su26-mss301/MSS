package com.wardrobe.common.auth;

public final class AuthContextHolder {

    private static final ThreadLocal<AuthContext> CONTEXT = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    public static void set(AuthContext authContext) {
        CONTEXT.set(authContext);
    }

    public static AuthContext get() {
        AuthContext authContext = CONTEXT.get();

        if (authContext == null) {
            throw new AuthException(
                    AuthErrorCode.AUTH_CONTEXT_MISSING,
                    "Auth context is not available"
            );
        }

        return authContext;
    }

    public static AuthContext getNullable() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}