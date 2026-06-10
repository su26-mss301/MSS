package com.wardrobe.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuthContextFilter extends OncePerRequestFilter {

    private static final List<String> DEFAULT_WHITELIST_PATTERNS = List.of(
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/**",
            "/actuator/health",
            "/eureka/**",
            "/favicon.ico"
    );

    private final AuthContextResolver authContextResolver;
    private final AntPathMatcher pathMatcher;
    private final ObjectMapper objectMapper;
    private final List<String> whitelistPatterns;

    public AuthContextFilter() {
        this(new AuthContextResolver(), DEFAULT_WHITELIST_PATTERNS);
    }

    public AuthContextFilter(List<String> additionalWhitelistPatterns) {
        this(new AuthContextResolver(), mergeWhitelist(additionalWhitelistPatterns));
    }

    public AuthContextFilter(
            AuthContextResolver authContextResolver,
            List<String> whitelistPatterns
    ) {
        this.authContextResolver = authContextResolver;
        this.pathMatcher = new AntPathMatcher();
        this.objectMapper = new ObjectMapper();
        this.whitelistPatterns = whitelistPatterns == null
                ? DEFAULT_WHITELIST_PATTERNS
                : List.copyOf(whitelistPatterns);
    }

    private static List<String> mergeWhitelist(List<String> additionalWhitelistPatterns) {
        List<String> patterns = new ArrayList<>(DEFAULT_WHITELIST_PATTERNS);

        if (additionalWhitelistPatterns != null) {
            patterns.addAll(additionalWhitelistPatterns);
        }

        return patterns;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return whitelistPatterns.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            AuthContext authContext = authContextResolver.resolve(request);
            AuthContextHolder.set(authContext);

            filterChain.doFilter(request, response);
        } catch (AuthException ex) {
            handleAuthException(response, ex);
        } finally {
            AuthContextHolder.clear();
        }
    }

    private void handleAuthException(
            HttpServletResponse response,
            AuthException ex
    ) throws IOException {
        HttpStatus status = switch (ex.getErrorCode()) {
            case AUTH_CONTEXT_MISSING, AUTH_CONTEXT_INVALID -> HttpStatus.UNAUTHORIZED;
            case AUTH_FORBIDDEN_ROLE, AUTH_FORBIDDEN_SCOPE -> HttpStatus.FORBIDDEN;
        };

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", ex.getErrorCode().name());
        body.put("message", ex.getMessage());

        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), body);
    }
}