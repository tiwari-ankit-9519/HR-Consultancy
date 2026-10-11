
package com.hrconsultancy.organization.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private static final Logger logger = LogManager.getLogger(RequestCorrelationFilter.class);

    private static final String REQUEST_ID_HEADER = "X-Request-ID";

    private static final String REQUEST_ID_KEY = "requestId";
    private static final String HTTP_METHOD_KEY = "httpMethod";
    private static final String REQUEST_PATH_KEY = "requestPath";

    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Preserve the MDC context that existed before this request.
        Map<String, String> previousContext = new HashMap<>(ThreadContext.getImmutableContext());

        String requestId = resolveRequestId(
                request.getHeader(REQUEST_ID_HEADER));

        long startTime = System.nanoTime();

        try {
            ThreadContext.put(REQUEST_ID_KEY, requestId);
            ThreadContext.put(HTTP_METHOD_KEY, request.getMethod());
            ThreadContext.put(REQUEST_PATH_KEY, request.getRequestURI());

            response.setHeader(REQUEST_ID_HEADER, requestId);

            logger.info(
                    "HTTP request started remoteAddress={}",
                    request.getRemoteAddr());

            filterChain.doFilter(request, response);

        } catch (IOException | ServletException | RuntimeException exception) {
            logger.error("HTTP request processing failed", exception);
            throw exception;

        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;

            logger.info(
                    "HTTP request completed status={} durationMs={}",
                    response.getStatus(),
                    durationMs);

            // Remove this request's MDC values and restore the previous context.
            ThreadContext.clearMap();
            ThreadContext.putAll(previousContext);
        }
    }

    private String resolveRequestId(String incomingRequestId) {

        if (incomingRequestId != null
                && SAFE_REQUEST_ID.matcher(incomingRequestId).matches()) {
            return incomingRequestId;
        }

        return UUID.randomUUID().toString();
    }
}
