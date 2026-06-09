package wardrobe.project.com.userservice.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class CookieBearerTokenResolver implements BearerTokenResolver {

    @Override
    public String resolve(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            System.out.println("USING AUTHORIZATION HEADER");
            return authHeader.substring(7);
        }

        String path = request.getRequestURI();
        System.out.println("REQUEST PATH = " + path);

        if (path.contains("/users/me")) {
            System.out.println("USING ID TOKEN COOKIE");
            return getCookieValue(request, "id_token");
        }

        System.out.println("USING ACCESS TOKEN COOKIE");
        return getCookieValue(request, "access_token");
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