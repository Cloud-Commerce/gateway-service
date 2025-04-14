package edu.ecom.gateway.security.filter;

import edu.ecom.gateway.security.filter.CustomAuthGatewayFilterFactory.Config;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CustomAuthGatewayFilterFactory extends
    AbstractGatewayFilterFactory<Config> {

//  private final AuthzService authzService;
//
//  public CustomAuthGatewayFilterFactory(AuthzService authzService) {
//    super(Config.class);
//    this.authzService = authzService;
//  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      String token = exchange.getRequest().getHeaders().getFirst("Authorization");

      if (token == null || !token.startsWith("Bearer ")) {
        return unauthorized(exchange, "Missing token");
      }

      token = token.substring(7);

//      return Mono.just(token)
//          .flatMap(validationResult -> {
//                if (validationResult == null) {
//                  return unauthorized(exchange, "Invalid token");
//                }
//
//                // Add user info to headers for downstream services
//                exchange.getRequest().mutate()
//                    .header("X-User-ID", "validationResult.getUserId()")
//                    .header("X-User-Roles", String.join(",", "validationResult.getRoles()"))
//                    .build();
//            return Mono.void();
//          });

            // Check route permissions

      return Mono.just(token)
          .flatMap(validationResult -> {
//            if (!validationResult.isValid()) {
//              return unauthorized(exchange, "Invalid token");
//            }

            // Check if user has required roles from route config
            if (!Collections.disjoint(
                Arrays.asList(config.getRequiredRoles()),
                List.of())) {
              return forbidden(exchange, "Missing required role");
            }

            return chain.filter(exchange);
          });
    };
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

  @Getter
  public static class Config {
    private String[] requiredRoles;

    // Getters and setters
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