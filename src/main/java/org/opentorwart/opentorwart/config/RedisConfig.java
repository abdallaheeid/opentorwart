package org.opentorwart.opentorwart.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean
    RedisScript<Long> reserveScript() {
        return RedisScript.of(new ClassPathResource("scripts/reserve.lua"), Long.class);
    }

    @Bean(destroyMethod = "shutdown")
    RedisClient bucketRedisClient(@Value("${spring.data.redis.host}") String host,
                                  @Value("${spring.data.redis.port}") String port) {
        return RedisClient.create(RedisURI.builder().withHost(host).withPort(Integer.parseInt(port)).build());
    }

    @Bean(destroyMethod = "close")
    StatefulRedisConnection<String, byte[]> bucketRedisConnection(RedisClient bucketRedisClient) {
        return bucketRedisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
    }

    @Bean
    ProxyManager<String> bucketProxyManager(StatefulRedisConnection<String, byte[]> connection) {
        return LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(1)))
                .build();
    }
}