package io.github.rivon0507.courier.common.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.time.Instant;
import java.util.Optional;

public class ProblemDetailsFactory {
    /**
     * Creates a {@link ProblemDetail} and enriches it with common metadata.
     *
     * <p>Common properties added by {@link #enrich(ProblemDetail, String, HttpServletRequest)}:
     * <ul>
     *   <li>{@code timestamp}: ISO-8601 timestamp</li>
     *   <li>{@code path}: request path</li>
     *   <li>{@code code}: machine-friendly error code</li>
     *   <li>{@code traceId}: optional request correlation identifier</li>
     * </ul>
     *
     * @param status  the HTTP status
     * @param code    a stable, machine-friendly error code
     * @param message a human-friendly message
     * @param details structured details (validation details, domain details, etc.)
     * @param req     the current HTTP request
     * @return a fully-populated {@link ProblemDetail}
     * @see ErrorCodes
     */
    public static @NonNull ProblemDetail problem(HttpStatus status,
                                                 String code,
                                                 String message,
                                                 @Nullable Object details,
                                                 @NonNull HttpServletRequest req) {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, message);
        pd.setTitle(status.getReasonPhrase());
        enrich(pd, code, req);
        if (details != null) pd.setProperty("details", details);
        return pd;
    }

    /**
     * Enriches a {@link ProblemDetail} with common metadata and the error code.
     *
     * @param pd   the problem detail to enrich
     * @param code the stable machine-friendly error code
     * @param req  the current HTTP request
     */
    public static void enrich(@NonNull ProblemDetail pd, @NonNull String code, @NonNull HttpServletRequest req) {
        pd.setProperty("timestamp", Instant.now().toString());
        pd.setProperty("path", req.getRequestURI());
        pd.setProperty("code", code);

        String traceId = traceId(req);
        if (traceId != null && !traceId.isBlank()) pd.setProperty("traceId", traceId);
    }

    /**
     * Extracts a request correlation identifier from common headers.
     *
     * @param req the current HTTP request
     * @return a trace/correlation id, or {@code null} if none is present
     */
    public static @Nullable String traceId(@NonNull HttpServletRequest req) {
        return Optional.ofNullable(req.getHeader("X-Request-Id"))
                .or(() -> Optional.ofNullable(req.getHeader("X-Correlation-Id")))
                .or(() -> Optional.ofNullable(req.getHeader("traceparent")))
                .orElse(null);
    }

    /**
     * Common API error codes.
     *
     * <p>These codes should be treated as part of the public API contract.
     */
    public static final class ErrorCodes {
        private ErrorCodes() {
        }

        public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
        public static final String REQUEST_FAILED = "REQUEST_FAILED";
        public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    }
}
