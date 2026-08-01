package com.finpay.gateway.filter;

import com.finpay.gateway.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/google",
            "/api/auth/verify-email",
            "/api/auth/forgot-password",
            "/api/auth/refresh-token",
            "/api/loans/calculate-emi",
            "/api/ai/chat/quick"
    );

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            if (HttpMethod.OPTIONS.equals(request.getMethod()) || isPublicPath(path)) {
                return chain.filter(exchange);
            }

            // Actuator and discovery health probes stay open.
            if (path.startsWith("/actuator")) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized(exchange, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7).trim();
            if (token.isEmpty() || !jwtUtil.validateToken(token)) {
                return unauthorized(exchange, "Invalid or expired JWT");
            }

            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Email", nullToEmpty(jwtUtil.extractEmail(token)))
                    .header("X-User-Role", nullToEmpty(jwtUtil.extractRole(token)))
                    .header("X-User-Id", nullToEmpty(jwtUtil.extractUserId(token)))
                    .build();

            return chain.filter(exchange.mutate().request(mutated).build());
        };
    }

    private boolean isPublicPath(String path) {
        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        // Tolerate trailing slashes from some clients.
        return PUBLIC_PATHS.contains(path.endsWith("/") ? path.substring(0, path.length() - 1) : path);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("{\"error\":\"unauthorized\",\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public static class Config {
        // Reserved for future per-route options (e.g. extra public paths).
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of();
    }
}
