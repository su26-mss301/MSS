package wardrobe.project.com.apigateway.dashboard.support;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class GatewayAuthSupport {

    private static final String HEADER_ACTOR_TYPE = "X-Auth-Actor-Type";
    private static final String HEADER_USER_ID = "X-Auth-User-Id";
    private static final String HEADER_USER_EMAIL = "X-Auth-User-Email";
    private static final String HEADER_USERNAME = "X-Auth-Username";
    private static final String HEADER_ROLE = "X-Auth-Role";
    private static final String HEADER_GROUPS = "X-Auth-Groups";
    private static final String HEADER_SCOPES = "X-Auth-Scopes";
    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    private final ReactiveJwtDecoder jwtDecoder;

    public GatewayAuthSupport(ReactiveJwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Getter
    @Builder
    public static class ForwardedAuth {
        private String role;
        private HttpHeaders headers;
    }

    public Mono<ForwardedAuth> resolveForwardedAuth(ServerHttpRequest request) {
        String roleFromHeader = request.getHeaders().getFirst(HEADER_ROLE);
        if (roleFromHeader != null && !roleFromHeader.isBlank()) {
            return Mono.just(ForwardedAuth.builder()
                    .role(roleFromHeader)
                    .headers(copyForwardHeaders(request))
                    .build());
        }

        String accessToken = resolveAccessToken(request);
        if (accessToken == null || accessToken.isBlank()) {
            return Mono.error(unauthorized("Missing access token"));
        }

        return jwtDecoder.decode(accessToken)
                .map(jwt -> ForwardedAuth.builder()
                        .role(resolveMainRole(extractRoles(jwt)))
                        .headers(buildHeadersFromJwt(request, jwt))
                        .build())
                .onErrorMap(
                        org.springframework.security.oauth2.jwt.JwtException.class,
                        ex -> unauthorized("Invalid access token")
                );
    }

    public HttpHeaders copyForwardHeaders(ServerHttpRequest request) {
        HttpHeaders target = new HttpHeaders();
        request.getHeaders().forEach((name, values) -> {
            if (shouldForwardHeader(name)) {
                values.forEach(value -> target.add(name, value));
            }
        });

        if (!target.containsKey(HttpHeaders.COOKIE)) {
            String cookieHeader = request.getHeaders().getFirst(HttpHeaders.COOKIE);
            if (cookieHeader != null) {
                target.add(HttpHeaders.COOKIE, cookieHeader);
            }
        }

        return target;
    }

    private HttpHeaders buildHeadersFromJwt(ServerHttpRequest request, Jwt jwt) {
        HttpHeaders headers = copyForwardHeaders(request);

        Set<String> roles = extractRoles(jwt);
        String role = resolveMainRole(roles);
        String requestId = request.getHeaders().getFirst(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        headers.set(HEADER_ACTOR_TYPE, "USER");
        headers.set(HEADER_USER_ID, jwt.getSubject() != null ? jwt.getSubject() : "");
        headers.set(HEADER_USER_EMAIL, stringClaim(jwt, "email"));
        headers.set(HEADER_USERNAME, stringClaim(jwt, "preferred_username"));
        headers.set(HEADER_ROLE, role != null ? role : "");
        headers.set(HEADER_GROUPS, String.join(",", roles));
        headers.set(HEADER_SCOPES, stringClaim(jwt, "scope"));
        headers.set(HEADER_REQUEST_ID, requestId);

        return headers;
    }

    private boolean shouldForwardHeader(String name) {
        return HttpHeaders.COOKIE.equalsIgnoreCase(name)
                || HttpHeaders.AUTHORIZATION.equalsIgnoreCase(name)
                || HEADER_ACTOR_TYPE.equalsIgnoreCase(name)
                || HEADER_USER_ID.equalsIgnoreCase(name)
                || HEADER_USER_EMAIL.equalsIgnoreCase(name)
                || HEADER_USERNAME.equalsIgnoreCase(name)
                || HEADER_ROLE.equalsIgnoreCase(name)
                || HEADER_GROUPS.equalsIgnoreCase(name)
                || HEADER_SCOPES.equalsIgnoreCase(name)
                || HEADER_REQUEST_ID.equalsIgnoreCase(name);
    }

    private String resolveAccessToken(ServerHttpRequest request) {
        String authorizationHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        HttpCookie accessTokenCookie = request.getCookies().getFirst("access_token");
        if (accessTokenCookie != null && accessTokenCookie.getValue() != null) {
            return accessTokenCookie.getValue();
        }

        return null;
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

    private String stringClaim(Jwt jwt, String claimName) {
        Object value = jwt.getClaims().get(claimName);
        return value == null ? "" : value.toString();
    }

    private ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                message
        );
    }
}
