package edu.ecom.gateway.security.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TokenRelayFilter implements GlobalFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    // Extract from original request
    String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
    String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");

    // Add to downstream request
    exchange.getRequest().mutate()
        .header("Authorization", authHeader)
        .header("X-User-ID", userId)
        .build();

    return chain.filter(exchange);
  }
}