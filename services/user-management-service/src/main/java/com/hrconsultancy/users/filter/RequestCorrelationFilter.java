
package com.hrconsultancy.users.filter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

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

    private static final int MAX_LOG_BODY_BYTES = 8192;

    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    private static final String REDACTED = "[REDACTED]";

    private static final String[] SENSITIVE_FIELD_NAMES = {
            "password",
            "passwd",
            "token",
            "access_token",
            "refresh_token",
            "authorization",
            "client_secret",
            "secret",
            "apikey",
            "api_key",
            "gstin",
            "aadhaar",
            "aadharnumber",
            "pan",
            "email",
            "phone",
            "mobile",
            "dateofbirth",
            "dob"
    };

    private final ObjectMapper objectMapper;
    private final boolean bodyLoggingEnabled;

    public RequestCorrelationFilter(
            ObjectMapper objectMapper,
            @Value("${app.logging.http-bodies-enabled:false}") boolean bodyLoggingEnabled) {

        this.objectMapper = objectMapper;
        this.bodyLoggingEnabled = bodyLoggingEnabled;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Map<String, String> previousContext = new HashMap<>(ThreadContext.getImmutableContext());

        String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));

        long startTime = System.nanoTime();

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, MAX_LOG_BODY_BYTES);

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            ThreadContext.put(REQUEST_ID_KEY, requestId);
            ThreadContext.put(HTTP_METHOD_KEY, request.getMethod());
            ThreadContext.put(REQUEST_PATH_KEY, request.getRequestURI());

            responseWrapper.setHeader(REQUEST_ID_HEADER, requestId);

            logger.info(
                    "HTTP request started method={} path={} remoteAddress={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr());

            filterChain.doFilter(requestWrapper, responseWrapper);

        } catch (IOException | ServletException | RuntimeException exception) {

            logger.error(
                    "HTTP request processing failed method={} path={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    exception);

            throw exception;

        } finally {

            try {
                if (bodyLoggingEnabled) {
                    logRequestBody(requestWrapper);
                    logResponseBody(responseWrapper);
                }

                long durationMs = (System.nanoTime() - startTime) / 1_000_000;

                logger.info(
                        "HTTP response completed method={} path={} status={} durationMs={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        responseWrapper.getStatus(),
                        durationMs);

            } finally {
                try {
                    responseWrapper.copyBodyToResponse();
                } finally {
                    ThreadContext.clearMap();
                    ThreadContext.putAll(previousContext);
                }
            }
        }
    }

    private void logRequestBody(ContentCachingRequestWrapper request) {
        byte[] body = request.getContentAsByteArray();

        if (body.length == 0) {
            logger.debug("HTTP request body is empty or was not cached");
            return;
        }

        if (!isJsonContentType(request.getContentType())) {
            logger.info("HTTP request body omitted because content is not JSON");
            return;
        }

        logger.info(
                "HTTP request body={}",
                formatJsonBody(body, request.getCharacterEncoding()));
    }

    private void logResponseBody(ContentCachingResponseWrapper response) {
        byte[] body = response.getContentAsByteArray();

        if (body.length == 0) {
            logger.debug("HTTP response body is empty");
            return;
        }

        if (!isJsonContentType(response.getContentType())) {
            logger.info("HTTP response body omitted because content is not JSON");
            return;
        }

        logger.info(
                "HTTP response body={}",
                formatJsonBody(body, StandardCharsets.UTF_8.name()));
    }

    private String formatJsonBody(byte[] body, String encoding) {
        String charsetName = StringUtils.hasText(encoding)
                ? encoding
                : StandardCharsets.UTF_8.name();

        Charset charset = Charset.forName(charsetName);

        int length = Math.min(body.length, MAX_LOG_BODY_BYTES);

        String rawBody = new String(body, 0, length, charset);

        try {
            JsonNode root = objectMapper.readTree(rawBody);

            if (root == null) {
                return "[empty JSON]";
            }

            redactSensitiveFields(root);

            String result = objectMapper.writeValueAsString(root);

            if (body.length > MAX_LOG_BODY_BYTES) {
                return result + " [TRUNCATED: body exceeded log limit]";
            }

            return result;

        } catch (Exception exception) {
            logger.warn("Unable to parse JSON body for safe logging");
            return "[JSON body omitted because it could not be safely redacted]";
        }
    }

    private void redactSensitiveFields(JsonNode node) {
        if (node == null) {
            return;
        }

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;

            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.properties().iterator();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();

                if (isSensitiveField(field.getKey())) {
                    objectNode.put(field.getKey(), REDACTED);
                } else {
                    redactSensitiveFields(field.getValue());
                }
            }

        } else if (node.isArray()) {
            for (JsonNode element : node) {
                redactSensitiveFields(element);
            }
        }
    }

    private boolean isSensitiveField(String fieldName) {
        String normalizedName = fieldName.replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase(java.util.Locale.ROOT);

        for (String sensitiveName : SENSITIVE_FIELD_NAMES) {
            String normalizedSensitiveName = sensitiveName.replaceAll("[^A-Za-z0-9]", "")
                    .toLowerCase(java.util.Locale.ROOT);

            if (normalizedName.contains(normalizedSensitiveName)) {
                return true;
            }
        }

        return false;
    }

    private boolean isJsonContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }

        try {
            MediaType mediaType = MediaType.parseMediaType(contentType);

            return MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)
                    || mediaType.getSubtype().endsWith("+json");

        } catch (IllegalArgumentException exception) {
            return false;
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

