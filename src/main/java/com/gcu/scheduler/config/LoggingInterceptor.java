package com.gcu.scheduler.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interceptor (Interceptor design pattern) that logs every web request on the
 * way in and its status and duration on the way out. This is the base for the
 * entry/exit logging and cloud log shipping added in Milestone 4.
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);
    private static final String START_ATTR = "requestStartMillis";

    /**
     * Logs the request before the controller runs.
     *
     * @return always true so the request continues
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_ATTR, System.currentTimeMillis());
        String user = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous";
        log.info("Request in : {} {} (user={})", request.getMethod(), request.getRequestURI(), user);
        return true;
    }

    /**
     * Logs the response status and how long the request took.
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        Object start = request.getAttribute(START_ATTR);
        long elapsed = start instanceof Long s ? System.currentTimeMillis() - s : -1;
        log.info("Request out: {} {} -> {} in {} ms", request.getMethod(), request.getRequestURI(),
                response.getStatus(), elapsed);
        if (ex != null) {
            log.error("Request failed: {}", ex.getMessage(), ex);
        }
    }
}
