package edu.ecom.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig<T> {

  @Bean
  public RedisTemplate<String, T> redisTemplate(RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, T> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);

    // Key serializer
    template.setKeySerializer(new StringRedisSerializer());

    // Value serializer (JSON format)
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

    // Hash key serializer
    template.setHashKeySerializer(new StringRedisSerializer());

    // Hash value serializer
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

    template.afterPropertiesSet();
    return template;
  }
}
