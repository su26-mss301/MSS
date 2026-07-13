package wardrobe.project.com.apigateway.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import wardrobe.project.com.apigateway.service.BlockedUserCacheService;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class KeycloakAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String HEADER_ACTOR_TYPE = "X-Auth-Actor-Type";
    private static final String HEADER_USER_ID = "X-Auth-User-Id";
    private static final String HEADER_USER_EMAIL = "X-Auth-User-Email";
    private static final String HEADER_USERNAME = "X-Auth-Username";
    private static final String HEADER_ROLE = "X-Auth-Role";
    private static final String HEADER_GROUPS = "X-Auth-Groups";
    private static final String HEADER_SCOPES = "X-Auth-Scopes";
    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    private final ReactiveJwtDecoder jwtDecoder;
    private final BlockedUserCacheService blockedUserCacheService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final List<String> publicPaths = List.of(
            "/api/v1/users/auth/**",
            "/api/v1/auth/**",
            "/auth/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**"
    );



    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitizedExchange = removeClientAuthHeaders(exchange);

        String path = sanitizedExchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(sanitizedExchange);
        }

        String accessToken = resolveAccessToken(sanitizedExchange);

        if (accessToken == null || accessToken.isBlank()) {
            return writeError(
                    sanitizedExchange,
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_TOKEN_MISSING",
                    "Missing access token"
            );
        }

        return jwtDecoder.decode(accessToken)
                .flatMap(jwt -> checkBlockedAndForward(
                        sanitizedExchange,
                        chain,
                        jwt
                ))
                .onErrorResume(
                        org.springframework.security.oauth2.jwt.JwtException.class,
                        exception -> writeError(
                                sanitizedExchange,
                                HttpStatus.UNAUTHORIZED,
                                "AUTH_TOKEN_INVALID",
                                "Invalid access token"
                        )
                )
                .onErrorResume(
                        org.springframework.data.redis.RedisConnectionFailureException.class,
                        exception -> writeError(
                                sanitizedExchange,
                                HttpStatus.SERVICE_UNAVAILABLE,
                                "REDIS_UNAVAILABLE",
                                "Authentication status service is unavailable"
                        )
                );
    }

    private Mono<Void> validateAndForward(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            Jwt jwt
    ) {
        String userId = jwt.getSubject();
        String email = getStringClaim(jwt, "email");
        String username = getStringClaim(jwt, "preferred_username");
        String scopes = getStringClaim(jwt, "scope");

        Set<String> roles = extractRoles(jwt);

        String role = resolveMainRole(roles);
        if (role == null) {
            return writeError(
                    exchange,
                    HttpStatus.FORBIDDEN,
                    "AUTH_ROLE_MISSING",
                    "User does not have required role"
            );
        }

        String requestId = exchange.getRequest().getHeaders().getFirst(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        String finalEmail = email == null ? "" : email;
        String finalUsername = username == null ? "" : username;
        String finalScopes = scopes == null ? "" : scopes;
        String finalRoles = String.join(",", roles);
        String finalRequestId = requestId;

        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.set(HEADER_ACTOR_TYPE, "USER");
                    headers.set(HEADER_USER_ID, userId);
                    headers.set(HEADER_USER_EMAIL, finalEmail);
                    headers.set(HEADER_USERNAME, finalUsername);
                    headers.set(HEADER_ROLE, role);
                    headers.set(HEADER_GROUPS, finalRoles);
                    headers.set(HEADER_SCOPES, finalScopes);
                    headers.set(HEADER_REQUEST_ID, finalRequestId);
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private Mono<Void> checkBlockedAndForward(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            Jwt jwt
    ) {
        String userId = jwt.getSubject();

        if (userId == null || userId.isBlank()) {
            return writeError(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_USER_ID_MISSING",
                    "Access token does not contain subject"
            );
        }

        return blockedUserCacheService.isBlocked(userId)
                .flatMap(blocked -> {
                    if (Boolean.TRUE.equals(blocked)) {
                        return writeError(
                                exchange,
                                HttpStatus.FORBIDDEN,
                                "USER_BLOCKED",
                                "User account is blocked or inactive"
                        );
                    }

                    return validateAndForward(
                            exchange,
                            chain,
                            jwt
                    );
                });
    }

    private Set<String> extractRoles(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> realmRoles) {
            realmRoles.forEach(role -> roles.add(role.toString()));
        }

        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null) {
            resourceAccess.values().forEach(clientAccess -> {
                if (clientAccess instanceof Map<?, ?> map
                        && map.get("roles") instanceof Collection<?> clientRoles) {
                    clientRoles.forEach(role -> roles.add(role.toString()));
                }
            });
        }

        return roles.stream()
                .filter(role -> role.startsWith("ROLE_"))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String resolveMainRole(Set<String> roles) {
        if (roles.contains("ROLE_ADMIN")) {
            return "ROLE_ADMIN";
        }

        if (roles.contains("ROLE_STAFF")) {
            return "ROLE_STAFF";
        }

        if (roles.contains("ROLE_USER")) {
            return "ROLE_USER";
        }

        return null;
    }

    private String resolveAccessToken(ServerWebExchange exchange) {
        String authorizationHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        HttpCookie accessTokenCookie = exchange.getRequest()
                .getCookies()
                .getFirst("access_token");

        if (accessTokenCookie != null && accessTokenCookie.getValue() != null) {
            return accessTokenCookie.getValue();
        }

        return null;
    }

    private ServerWebExchange removeClientAuthHeaders(ServerWebExchange exchange) {
        ServerHttpRequest sanitizedRequest = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.remove(HEADER_ACTOR_TYPE);
                    headers.remove(HEADER_USER_ID);
                    headers.remove(HEADER_USER_EMAIL);
                    headers.remove(HEADER_USERNAME);
                    headers.remove(HEADER_ROLE);
                    headers.remove(HEADER_GROUPS);
                    headers.remove(HEADER_SCOPES);
                })
                .build();

        return exchange.mutate().request(sanitizedRequest).build();
    }

    private boolean isPublicPath(String path) {
        return publicPaths.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String getStringClaim(Jwt jwt, String claimName) {
        Object value = jwt.getClaims().get(claimName);
        return value == null ? null : value.toString();
    }

    private Mono<Void> writeError(
            ServerWebExchange exchange,
            HttpStatus status,
            String error,
            String message
    ) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");

        String body = """
                {
                  "status": %d,
                  "error": "%s",
                  "message": "%s"
                }
                """.formatted(status.value(), error, message);

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(bytes))
        );
    }

    @Override
    public int getOrder() {
        return -100;
    }
}