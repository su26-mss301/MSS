package wardrobe.project.com.apigateway.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import wardrobe.project.com.apigateway.dashboard.dto.AdminDashboardOverviewResponse;
import wardrobe.project.com.apigateway.dashboard.service.AdminDashboardService;
import wardrobe.project.com.apigateway.dashboard.support.GatewayAuthSupport;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final GatewayAuthSupport gatewayAuthSupport;

    @GetMapping("/overview")
    public Mono<AdminDashboardOverviewResponse> getOverview(
            ServerWebExchange exchange,
            @RequestParam(defaultValue = "week") String granularity
    ) {
        return gatewayAuthSupport.resolveForwardedAuth(exchange.getRequest())
                .flatMap(auth -> {
                    if (!"ROLE_ADMIN".equals(auth.getRole())) {
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.FORBIDDEN,
                                "Admin access required"
                        ));
                    }

                    return adminDashboardService.getOverview(auth.getHeaders(), granularity);
                });
    }
}
