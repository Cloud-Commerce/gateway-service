package edu.ecom.gateway.security.filter;

import edu.ecom.authz.security.dto.TokenDetails;
import edu.ecom.authz.security.service.RequiredRoleFinderService;
import edu.ecom.authz.security.service.RoleBasedAuthorizationService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CustomAuthFilter implements GlobalFilter {

  private final RoleBasedAuthorizationService authzService; // From your authz-lib
  private final RequiredRoleFinderService roleFinderService;

  @Autowired
  public CustomAuthFilter(RoleBasedAuthorizationService authzService,
      RequiredRoleFinderService roleFinderService) {
    this.authzService = authzService;
    this.roleFinderService = roleFinderService;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getPath().toString();

    // Skip authentication for public endpoints
    if (isPublicEndpoint(path)) {
      return chain.filter(exchange);
    }

    // Extract authHeader from header
    String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
    if (Optional.ofNullable(authHeader).filter(t -> t.startsWith("Bearer ")).isEmpty()) {
      return unauthorized(exchange, "Missing or invalid Authorization header");
    }

    String token = authHeader.split(" ")[1];

//     Validate authHeader using your authz-lib
    Mono<TokenDetails> authorizedClaims = authzService.getAuthorizedClaims(token,
        roleFinderService.getRequiredRoleForRoute(path));
    return authorizedClaims
        .flatMap(tokenDetails -> {
          if (!tokenDetails.isGenuine()) {
            return unauthorized(exchange, "Invalid authHeader");
          }

          // Add user info to headers for downstream services
          exchange.getRequest().mutate()
              .header("Authorization", "Bearer " + tokenDetails.getToken())
              .header("X-User-ID", tokenDetails.getUsername())
              .header("X-User-Roles", String.join(",", tokenDetails.getRoles()))
              .build();

          // Check route permissions
//          if (!hasRequiredPermissions(path, tokenDetails.getRoles())) {
//            return forbidden(exchange, "Insufficient permissions");
//          }

          return chain.filter(exchange);
        })
        .onErrorResume(e -> serverError(exchange, "Authentication service error: " + e.getMessage()));
  }

  private boolean isPublicEndpoint(String path) {
    return path.startsWith("/public/") || path.startsWith("/api/auth/") || List.of("/health", "/info").contains(path);
  }

  private boolean hasRequiredPermissions(String path, Set<String> userRoles) {
    // Implement your permission logic here
    // Could integrate with your authz-lib's permission checks
    return true;
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    return exchange.getResponse().writeWith(Mono.just(
        exchange.getResponse().bufferFactory().wrap(
            ("{\"error\": \"" + message + "\"}").getBytes()
        )
    ));
  }

  private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
    // Similar to unauthorized but with 403 status
    return unauthorized(exchange, message);
  }

  private Mono<Void> serverError(ServerWebExchange exchange, String message) {
    // Similar to unauthorized but with 503 status
    return unauthorized(exchange, message);
  }
}