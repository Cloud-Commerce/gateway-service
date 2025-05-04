package edu.ecom.gateway.client;

import edu.ecom.authz.security.dto.TokenDetails;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceClient {

  private final WebClient.Builder webClientBuilder;

  public Mono<TokenDetails> verifyToken(String token) {
    return webClientBuilder.build()
        .post()
        .uri("http://authn-service/api/auth/verify")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, response ->
            response.bodyToMono(ErrorMessage.class)
                .defaultIfEmpty(new ErrorMessage("Client error"))
                .flatMap(body -> Mono.error(new RuntimeException(
                    "Client error: " + body.message() + " (status: " + response.statusCode() + ")"))))
        .onStatus(HttpStatusCode::is5xxServerError, response ->
            response.bodyToMono(ErrorMessage.class)
                .defaultIfEmpty(new ErrorMessage("Server error"))
                .flatMap(body -> Mono.error(new RuntimeException(
                    "Server error: " + body.message() + " (status: " + response.statusCode() + ")"))))
        .bodyToMono(TokenDetails.class)
        .timeout(Duration.ofSeconds(3))
        .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
        .onErrorResume(ex -> {
          if (Exceptions.isRetryExhausted(ex)) {
            return Mono.error(new RuntimeException(
                "Service unavailable after 3 retries. Last error: " + ex.getCause().getMessage()));
          }
          return Mono.error(ex);
        });
  }

  record ErrorMessage(String message) {}

  static class AuthClientException extends RuntimeException {

    public AuthClientException(String message) {
      super(message);
    }
  }

  static class AuthServerException extends RuntimeException {

    public AuthServerException(String message) {
      super(message);
    }
  }

  static class AuthServiceException extends RuntimeException {

    public AuthServiceException(String message) {
      super(message);
    }
  }
}