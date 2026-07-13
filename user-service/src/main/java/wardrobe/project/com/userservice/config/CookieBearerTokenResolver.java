package wardrobe.project.com.userservice.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/auth/login",
            "/auth/register",
            "/auth/confirm-register",
            "/auth/resend-code",
            "/auth/google/callback",
            "/auth/refresh",
            "/auth/forgot-password",
            "/auth/verify-forgot-password-otp",
            "/auth/reset-password"
    );

    @Override
    public String resolve(HttpServletRequest request) {
        String path = request.getRequestURI();

        System.out.println("REQUEST PATH = " + path);

        // API public không được cố xác thực access_token cũ
        if (isPublicPath(path)) {
            System.out.println("PUBLIC PATH - SKIP ACCESS TOKEN");
            return null;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            System.out.println("USING AUTHORIZATION HEADER");
            return authHeader.substring(7);
        }

        System.out.println("USING ACCESS TOKEN COOKIE");
        return getCookieValue(request, "access_token");
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::endsWith);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}