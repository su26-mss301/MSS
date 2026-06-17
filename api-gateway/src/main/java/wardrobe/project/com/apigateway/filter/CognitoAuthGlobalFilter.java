package wardrobe.project.com.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import wardrobe.project.com.apigateway.config.CognitoProperties;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Component
public class CognitoAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String HEADER_ACTOR_TYPE = "X-Auth-Actor-Type";
    private static final String HEADER_USER_ID = "X-Auth-User-Id";
    private static final String HEADER_USER_EMAIL = "X-Auth-User-Email";
    private static final String HEADER_ROLE = "X-Auth-Role";
    private static final String HEADER_GROUPS = "X-Auth-Groups";
    private static final String HEADER_SCOPES = "X-Auth-Scopes";
    private static final String HEADER_REQUEST_ID = "X-Request-Id";

    private final ReactiveJwtDecoder jwtDecoder;
    private final CognitoProperties cognitoProperties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private final List<String> publicPaths = List.of(
            "/api/v1/users/auth/**",
            "/api/v1/auth/**",
            "/auth/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**"
    );

    public CognitoAuthGlobalFilter(
            ReactiveJwtDecoder jwtDecoder,
            CognitoProperties cognitoProperties
    ) {
        this.jwtDecoder = jwtDecoder;
        this.cognitoProperties = cognitoProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitizedExchange = removeClientAuthHeaders(exchange);
        String path = sanitizedExchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(sanitizedExchange);
        }

        TokenPair tokenPair = resolveTokens(sanitizedExchange);

        if (tokenPair.accessToken() == null || tokenPair.accessToken().isBlank()) {
            return writeError(
                    sanitizedExchange,
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_TOKEN_MISSING",
                    "Missing access token"
            );
        }

        Mono<Jwt> accessJwtMono = jwtDecoder.decode(tokenPair.accessToken());

        if (tokenPair.idToken() == null || tokenPair.idToken().isBlank()) {
            return accessJwtMono
                    .flatMap(accessJwt -> validateAndForward(
                            sanitizedExchange,
                            chain,
                            accessJwt,
                            null
                    ))
                    .onErrorResume(ex -> writeError(
                            sanitizedExchange,
                            HttpStatus.UNAUTHORIZED,
                            "AUTH_TOKEN_INVALID",
                            "Invalid access token or unable to obtain Cognito public keys"
                    ));
        }

        Mono<Jwt> idJwtMono = jwtDecoder.decode(tokenPair.idToken());

        return Mono.zip(accessJwtMono, idJwtMono)
                .flatMap(tuple -> validateAndForward(
                        sanitizedExchange,
                        chain,
                        tuple.getT1(),
                        tuple.getT2()
                ))
                .onErrorResume(ex -> writeError(
                        sanitizedExchange,
                        HttpStatus.UNAUTHORIZED,
                        "AUTH_TOKEN_INVALID",
                        "Invalid access token, id token, or unable to obtain Cognito public keys"
                ));
    }

    private Mono<Void> validateAndForward(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            Jwt accessJwt,
            Jwt idJwt
    ) {
        String issuer = accessJwt.getIssuer() != null ? accessJwt.getIssuer().toString() : null;
        String tokenUse = accessJwt.getClaimAsString("token_use");
        String clientId = accessJwt.getClaimAsString("client_id");
        String userId = accessJwt.getSubject();
        String scopes = accessJwt.getClaimAsString("scope");

        if (!cognitoProperties.getIssuerUri().equals(issuer)) {
            return writeError(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_TOKEN_INVALID_ISSUER",
                    "Invalid token issuer"
            );
        }

        if (!"access".equals(tokenUse)) {
            return writeError(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_TOKEN_INVALID_TYPE",
                    "Token must be an access token"
            );
        }

        if (!cognitoProperties.getClientId().equals(clientId)) {
            return writeError(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "AUTH_TOKEN_INVALID_CLIENT",
                    "Invalid token client id"
            );
        }

        String email = "";

        if (idJwt != null) {
            String idIssuer = idJwt.getIssuer() != null ? idJwt.getIssuer().toString() : null;
            String idTokenUse = idJwt.getClaimAsString("token_use");
            String idAudience = idJwt.getAudience() != null && !idJwt.getAudience().isEmpty()
                    ? idJwt.getAudience().get(0)
                    : null;
            String idSubject = idJwt.getSubject();

            if (!cognitoProperties.getIssuerUri().equals(idIssuer)) {
                return writeError(
                        exchange,
                        HttpStatus.UNAUTHORIZED,
                        "AUTH_ID_TOKEN_INVALID_ISSUER",
                        "Invalid id token issuer"
                );
            }

            if (!"id".equals(idTokenUse)) {
                return writeError(
                        exchange,
                        HttpStatus.UNAUTHORIZED,
                        "AUTH_ID_TOKEN_INVALID_TYPE",
                        "Token must be an id token"
                );
            }

            if (!cognitoProperties.getClientId().equals(idAudience)) {
                return writeError(
                        exchange,
                        HttpStatus.UNAUTHORIZED,
                        "AUTH_ID_TOKEN_INVALID_AUDIENCE",
                        "Invalid id token audience"
                );
            }

            if (!userId.equals(idSubject)) {
                return writeError(
                        exchange,
                        HttpStatus.UNAUTHORIZED,
                        "AUTH_TOKEN_SUB_MISMATCH",
                        "Access token and id token belong to different users"
                );
            }

            String emailClaim = idJwt.getClaimAsString("email");
            email = emailClaim == null ? "" : emailClaim;
        }

        List<String> groups = accessJwt.getClaimAsStringList("cognito:groups");
        if (groups == null) {
            groups = List.of();
        }

        String role = resolveRole(groups);
        if (role == null) {
            return writeError(
                    exchange,
                    HttpStatus.FORBIDDEN,
                    "AUTH_ROLE_MISSING",
                    "User does not have required Cognito role"
            );
        }

        String requestId = exchange.getRequest().getHeaders().getFirst(HEADER_REQUEST_ID);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        String finalScopes = scopes == null ? "" : scopes;
        String finalGroups = String.join(",", groups);
        String finalRequestId = requestId;
        String finalEmail = email;

        ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .headers(headers -> {
                    headers.set(HEADER_ACTOR_TYPE, "USER");
                    headers.set(HEADER_USER_ID, userId);
                    headers.set(HEADER_USER_EMAIL, finalEmail);
                    headers.set(HEADER_ROLE, role);
                    headers.set(HEADER_GROUPS, finalGroups);
                    headers.set(HEADER_SCOPES, finalScopes);
                    headers.set(HEADER_REQUEST_ID, finalRequestId);
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private String resolveRole(List<String> groups) {
        if (groups.contains("ROLE_ADMIN")) {
            return "ROLE_ADMIN";
        }

        if (groups.contains("ROLE_USER")) {
            return "ROLE_USER";
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

    private record TokenPair(String accessToken, String idToken) {
    }

    private TokenPair resolveTokens(ServerWebExchange exchange) {
        String accessToken = null;
        String idToken = null;

        String authorizationHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            accessToken = authorizationHeader.substring(7);
        }

        HttpCookie accessTokenCookie = exchange.getRequest()
                .getCookies()
                .getFirst("access_token");

        if ((accessToken == null || accessToken.isBlank())
                && accessTokenCookie != null
                && accessTokenCookie.getValue() != null) {
            accessToken = accessTokenCookie.getValue();
        }

        HttpCookie idTokenCookie = exchange.getRequest()
                .getCookies()
                .getFirst("id_token");

        if (idTokenCookie != null && idTokenCookie.getValue() != null) {
            idToken = idTokenCookie.getValue();
        }

        return new TokenPair(accessToken, idToken);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}