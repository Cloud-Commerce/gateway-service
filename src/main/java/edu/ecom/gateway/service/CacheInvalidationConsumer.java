package edu.ecom.gateway.service;

import edu.ecom.authz.security.dto.TokenDetails;
import edu.ecom.common.events.CacheInvalidationEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CacheInvalidationConsumer {
    private final RedisTemplate<String, TokenDetails> redisTemplate;

    @KafkaListener(topics = "auth.cache.invalidation")
    public void handleCacheInvalidation(CacheInvalidationEvent event) {
        log.info("Received invalidation event: {}", event);
        
        switch (event.operation()) {
            case "PASSWORD_CHANGE" :
            case "LOGOUT" :
                evictCache(event.ids());
                break;
            default :
                log.warn("Unknown operation: {}", event.operation());
        }
    }

    private void evictCache(List<String> ids) {
        ids.stream().map(TokenSessionManagementService::getVerifiedSessionKey).forEach(redisTemplate::delete);
        log.debug("Cache evicted for {}", ids);
    }

}