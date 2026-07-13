package wardrobe.project.com.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import wardrobe.project.com.apigateway.service.BlockedUserCacheService;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BlockedUserGlobalFilter implements GlobalFilter, Ordered {

    private final BlockedUserCacheService blockedUserCacheService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .flatMap(authentication -> {
                    if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
                        return chain.filter(exchange);
                    }

                    String userId =
                            jwtAuthentication.getToken().getSubject();

                    if (userId == null || userId.isBlank()) {
                        return chain.filter(exchange);
                    }

                    return blockedUserCacheService
                            .isBlocked(userId)
                            .flatMap(blocked -> {
                                if (Boolean.TRUE.equals(blocked)) {
                                    return writeBlockedResponse(exchange);
                                }

                                return chain.filter(exchange);
                            });
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private Mono<Void> writeBlockedResponse(
            ServerWebExchange exchange
    ) {
        var response = exchange.getResponse();

        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(
                MediaType.APPLICATION_JSON
        );

        Map<String, Object> responseBody = Map.of(
                "success", false,
                "code", 403,
                "message", "Tài khoản đã bị khóa hoặc không hoạt động"
        );

        try {
            byte[] bytes = objectMapper
                    .writeValueAsString(responseBody)
                    .getBytes(StandardCharsets.UTF_8);

            var buffer = response
                    .bufferFactory()
                    .wrap(bytes);

            return response.writeWith(Mono.just(buffer));

        } catch (JsonProcessingException exception) {
            return response.setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -50;
    }
}