package com.clsaa.dop.server.user.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * ????????????
 *
 * @author joyren
 */
@Configuration
@EnableCaching
public class CacheConfig extends CachingConfigurerSupport {
    /**
     * ???????????????????????????????????????
     * ????????? ?????????????????????????????????????????????????????????RedisCacheCustomizer#RedisCacheCustomizer????????????????????????????????????????????????0??????????????????
     *
     * @see CacheConfig#cacheManager(org.springframework.data.redis.core.RedisTemplate)
     */
    public interface CacheNames {
        /**
         * 15???????????????
         */
        String CACHE_EXPIRED_15_MINS = "dop:user:cache:15m";
        /**
         * 30???????????????
         */
        String CACHE_EXPIRED_30_MINS = "dop:user:cache:30m";
        /**
         * 60???????????????
         */
        String CACHE_EXPIRED_60_MINS = "dop:user:cache:60m";
        /**
         * 180???????????????
         */
        String CACHE_EXPIRED_180_MINS = "dop:user:cache:180m";

    }

    public class Constants {

        /**
         * ????????????????????????
         */
        static final String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
        /**
         * ??????????????????
         */
        static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
        /**
         * ??????????????????
         */
        static final String DEFAULT_TIME_FORMAT = "HH:mm:ss";

    }

    /**
     * ??????CacheManager
     */
    @Bean
    @SuppressWarnings("unchecked")
    public CacheManager cacheManager(RedisTemplate<?, ?> redisTemplate) {
        // RedisCache????????????RedisCacheWriter???????????????Redis
        RedisCacheWriter writer = RedisCacheWriter.lockingRedisCacheWriter(redisTemplate.getConnectionFactory());
        // SerializationPair??????Java?????????Redis?????????????????????????????????
        // Spring Boot????????????JdkSerializationRedisSerializer?????????????????????????????????
        // ???????????????????????????redis???????????????????????????????????????????????????Serializer???????????????????????????Serializable??????
        // ???????????????StringRedisSerializer???????????????????????????redis???key???
        RedisSerializationContext.SerializationPair keySerializationPair = RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer());
        // ??????Jackson2JsonRedisSerializer???????????????????????????redis???value???
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(JacksonConfig.Constants.DEFAULT_DATE_TIME_FORMAT)));
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(JacksonConfig.Constants.DEFAULT_DATE_FORMAT)));
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(JacksonConfig.Constants.DEFAULT_TIME_FORMAT)));
        javaTimeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(JacksonConfig.Constants.DEFAULT_DATE_TIME_FORMAT)));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(JacksonConfig.Constants.DEFAULT_DATE_FORMAT)));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(JacksonConfig.Constants.DEFAULT_TIME_FORMAT)));
        om.registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(javaTimeModule);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        RedisSerializationContext.SerializationPair valueSerializationPair = RedisSerializationContext.SerializationPair
                .fromSerializer(jackson2JsonRedisSerializer);
        // ????????????RedisCache?????????????????????????????????????????????Key???Value??????????????????
        // ?????????????????????????????????30s
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(30))
                .serializeKeysWith(keySerializationPair).serializeValuesWith(valueSerializationPair);
        Map<String, RedisCacheConfiguration> redisCacheConfigurationMap = ImmutableMap.<String, RedisCacheConfiguration>builder()
                .put(CacheNames.CACHE_EXPIRED_15_MINS,
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(15))
                                .serializeKeysWith(keySerializationPair).serializeValuesWith(valueSerializationPair))
                .put(CacheNames.CACHE_EXPIRED_30_MINS,
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(30))
                                .serializeKeysWith(keySerializationPair).serializeValuesWith(valueSerializationPair))
                .put(CacheNames.CACHE_EXPIRED_60_MINS,
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(60))
                                .serializeKeysWith(keySerializationPair).serializeValuesWith(valueSerializationPair))
                .put(CacheNames.CACHE_EXPIRED_180_MINS,
                        RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(180))
                                .serializeKeysWith(keySerializationPair).serializeValuesWith(valueSerializationPair))
                .build();
        return new RedisCacheManager(writer, config, redisCacheConfigurationMap);
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    static class LoggingCacheErrorHandler extends SimpleCacheErrorHandler {
        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        @Override
        public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
            logger.error(String.format("cacheName:%s,cacheKey:%s", cache == null ? "unknown" : cache.getName(), key), exception);
            super.handleCacheGetError(exception, cache, key);
        }

        @Override
        public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
            logger.error(String.format("cacheName:%s,cacheKey:%s", cache == null ? "unknown" : cache.getName(), key), exception);
            super.handleCachePutError(exception, cache, key, value);
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
            logger.error(String.format("cacheName:%s,cacheKey:%s", cache == null ? "unknown" : cache.getName(), key), exception);
            super.handleCacheEvictError(exception, cache, key);
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, Cache cache) {
            logger.error(String.format("cacheName:%s", cache == null ? "unknown" : cache.getName()), exception);
            super.handleCacheClearError(exception, cache);
        }
    }
}
