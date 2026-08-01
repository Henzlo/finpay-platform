package com.finpay.loan.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

@Aspect
@Component
@Slf4j
public class AuditLogAspect {

    @Around("execution(* com.finpay.loan.controller..*(..))")
    public Object auditController(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        String userId = resolveUserId();
        Instant startedAt = Instant.now();
        long startNanos = System.nanoTime();

        log.info("AUDIT start method={} userId={} timestamp={}", method, userId, startedAt);

        try {
            Object result = joinPoint.proceed();
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("AUDIT success method={} userId={} durationMs={} timestamp={}",
                    method, userId, elapsedMs, Instant.now());
            return result;
        } catch (Throwable ex) {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.warn("AUDIT failure method={} userId={} durationMs={} error={} timestamp={}",
                    method, userId, elapsedMs, ex.getMessage(), Instant.now());
            throw ex;
        }
    }

    private String resolveUserId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        String userId = request.getHeader("X-User-Id");
        return userId != null && !userId.isBlank() ? userId : "anonymous";
    }
}
