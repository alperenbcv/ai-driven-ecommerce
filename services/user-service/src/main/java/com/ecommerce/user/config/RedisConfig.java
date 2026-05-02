package com.ecommerce.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Redis yapılandırması — BloomFilter baytlarını saklamak için
 * byte[] değerli bir RedisTemplate ve async çalışma için @EnableAsync.
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, byte[]> bloomRedisTemplate(RedisConnectionFactory cf) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.byteArray());
        template.afterPropertiesSet();
        return template;
    }
}
