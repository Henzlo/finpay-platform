package com.finpay.gateway.config;

import com.finpay.gateway.filter.JwtAuthenticationFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    /**
     * Registers {@link JwtAuthenticationFilter} as an ordered {@link GlobalFilter}
     * so JWT checks apply to every route without listing the filter in YAML and
     * without pulling in Spring Security.
     */
    @Bean
    public GlobalFilter jwtAuthenticationGlobalFilter(JwtAuthenticationFilter jwtAuthenticationFilter) {
        return new OrderedJwtGlobalFilter(jwtAuthenticationFilter);
    }

    private static final class OrderedJwtGlobalFilter implements GlobalFilter, Ordered {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        private OrderedJwtGlobalFilter(JwtAuthenticationFilter jwtAuthenticationFilter) {
            this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            return jwtAuthenticationFilter
                    .apply(new JwtAuthenticationFilter.Config())
                    .filter(exchange, chain);
        }

        @Override
        public int getOrder() {
            return -100;
        }
    }
}
