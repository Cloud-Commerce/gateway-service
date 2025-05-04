package edu.ecom.common.events;

import java.time.Instant;
import java.util.List;

public record CacheInvalidationEvent(
    String eventId,       // UUID for deduplication
    String cacheName,     // "jwt_blacklist", "user_sessions"
    List<String> ids,          // JWT ID or user ID
    String operation,     // "LOGOUT", "PASSWORD_CHANGE"
    Instant timestamp
    ) {}