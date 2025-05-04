package edu.ecom.gateway.service;

import edu.ecom.authz.security.dto.TokenDetails;
import edu.ecom.gateway.client.AuthenticationServiceClient;
import edu.ecom.gateway.utils.JTIExtractor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class TokenSessionManagementService {

  @Value("${app.verify.session.expiration-ms}")
  private long sessionExpirationMs;

  private final RedisTemplate<String, TokenDetails> redisTemplate;
  private final AuthenticationServiceClient authenticator;

  @Autowired
  public TokenSessionManagementService(RedisTemplate<String, TokenDetails> redisTemplate,
      AuthenticationServiceClient authenticator) {
    this.redisTemplate = redisTemplate;
    this.authenticator = authenticator;
  }

  public Mono<TokenDetails> fetchSession(String token) {
    //     Validate authHeader using authz-lib
    return fetchCachedSession(token).switchIfEmpty(authenticator.verifyToken(token))
        .filter(TokenDetails::isGenuine)
        .flatMap(this::cacheVerifiedSession)  // Proper caching
        .doOnError(e -> System.err.println("Error fetching session: " + e.getMessage()))
        .onErrorResume(Mono::error);
  }
  private Mono<TokenDetails> cacheVerifiedSession(TokenDetails tokenDetails) {
    return Mono.fromRunnable(() -> {
      long ttlInMillis = Math.min(sessionExpirationMs, tokenDetails.getExpiration().getTime() - System.currentTimeMillis());
      redisTemplate.opsForValue().set(getVerifiedSessionKey(tokenDetails.getId()), tokenDetails, ttlInMillis, TimeUnit.MILLISECONDS);
    }).thenReturn(tokenDetails);  // Return the details after caching
  }

  private Mono<TokenDetails> fetchCachedSession(String token) {
    String jti = JTIExtractor.getJTI(token);
    return Mono.justOrEmpty(redisTemplate.opsForValue().get(getVerifiedSessionKey(jti)));
  }

  public static String getVerifiedSessionKey(String jti) {
    return String.format("user:session:verify:%s", jti);
  }

}