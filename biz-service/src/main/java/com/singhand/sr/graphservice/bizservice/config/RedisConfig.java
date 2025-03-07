package com.singhand.sr.graphservice.bizservice.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import jakarta.annotation.Nonnull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {

    final var objectMapper = createObjectMapper();

    final var serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);

    final var template = new RedisTemplate<String, Object>();
    template.setConnectionFactory(factory);

    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(serializer);
    template.afterPropertiesSet();
    return template;
  }

  private @Nonnull ObjectMapper createObjectMapper() {

    final var objectMapper = new ObjectMapper();
    objectMapper.setVisibility(PropertyAccessor.ALL, Visibility.ANY);

    objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
        ObjectMapper.DefaultTyping.NON_FINAL);

    return objectMapper;
  }
}
