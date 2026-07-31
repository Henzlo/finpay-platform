package com.finpay.gateway.config;

import com.finpay.gateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver userKeyResolver(JwtUtil jwtUtil) {
        return exchange -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();
                try {
                    if (jwtUtil.validateToken(token)) {
                        String userId = jwtUtil.extractUserId(token);
                        if (StringUtils.hasText(userId)) {
                            return Mono.just("user:" + userId);
                        }
                    }
                } catch (Exception ignored) {
                    // Fall through to IP-based key.
                }
            }

            String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (!StringUtils.hasText(ip) && exchange.getRequest().getRemoteAddress() != null
                    && exchange.getRequest().getRemoteAddress().getAddress() != null) {
                ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            }
            if (!StringUtils.hasText(ip)) {
                ip = "unknown";
            } else if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return Mono.just("ip:" + ip);
        };
    }

    @Bean
    public RedisRateLimiter defaultRedisRateLimiter() {
        return new RedisRateLimiter(50, 100);
    }
}
