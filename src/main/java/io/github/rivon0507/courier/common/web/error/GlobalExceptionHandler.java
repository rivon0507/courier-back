package io.github.rivon0507.courier.common.web.error;

import io.github.rivon0507.courier.common.web.error.ProblemDetailsFactory.ErrorCodes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static io.github.rivon0507.courier.common.web.error.ProblemDetailsFactory.problem;

/**
 * Global API exception handler for the courier backend.
 *
 * <p>This handler aims to provide:
 * <ul>
 *   <li>A single, uniform error response based on {@link ProblemDetail} (RFC 9457 style).</li>
 *   <li>Stable, machine-friendly error codes via the {@code code} property.</li>
 *   <li>Consistent metadata such as {@code timestamp}, {@code path}, and optional {@code traceId}.</li>
 *   <li>Structured validation errors, split into {@code fieldErrors}, {@code globalErrors}, and {@code parameterErrors}.</li>
 * </ul>
 *
 * <p>Note: authentication/authorization errors (401/403) are produced by Spring Security's filter chain and may need
 * explicit Security handlers (AuthenticationEntryPoint / AccessDeniedHandler) if you want bodies for those responses.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Handles application and domain errors explicitly raised by the codebase.
     *
     * <p>{@link ApiException} is intended to be the primary mechanism for declared, expected failures.
     * It carries the HTTP status, stable error code, a message, and optional details.
     *
     * @param ex  the application exception
     * @param req the current HTTP request
     * @return a {@link ProblemDetail} representing the failure
     */
    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex, HttpServletRequest req) {

        if (ex.status().is5xxServerError()) {
            log.error("ApiException code={} path={}", ex.code(), req.getRequestURI(), ex);
        } else {
            log.warn("ApiException code={} status={} path={} message={}",
                    ex.code(), ex.status().value(), req.getRequestURI(), ex.getMessage());
        }

        return problem(ex.status(), ex.code(), ex.getMessage(), null, req);
    }

    /**
     * Handles bean validation errors for {@code @Valid @RequestBody} payloads.
     *
     * <p>Produces a structured payload with:
     * <ul>
     *   <li>{@code code}: {@link ErrorCodes#VALIDATION_FAILED}</li>
     *   <li>{@code message}: a short summary</li>
     *   <li>{@code fieldErrors}: list of field-level validation failures</li>
     *   <li>{@code globalErrors}: list of object-level (global) validation failures</li>
     * </ul>
     *
     * @param ex      the validation exception raised for request bodies
     * @param headers the HTTP headers to include in the response
     * @param status  the HTTP status computed by Spring
     * @param request the current web request
     * @return a {@link ResponseEntity} containing the {@link ProblemDetail} body
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        HttpServletRequest servletReq = servletRequest(request);

        ValidationDetails details = new ValidationDetails(
                toFieldErrors(ex.getBindingResult().getFieldErrors()),
                toGlobalErrors(ex.getBindingResult().getGlobalErrors()),
                List.of()
        );

        String message = "Validation failed for object='%s'. Error count: %d"
                .formatted(ex.getBindingResult().getObjectName(), ex.getBindingResult().getErrorCount());

        ProblemDetail pd = problem(
                HttpStatus.BAD_REQUEST,
                ErrorCodes.VALIDATION_FAILED,
                message,
                details,
                servletReq
        );

        return ResponseEntity.badRequest().headers(headers).body(pd);
    }

    /**
     * Handles method parameter validation errors for handlers annotated with {@code @Validated}.
     *
     * <p>Produces {@code parameterErrors} in a structured payload. The {@code parameter} field corresponds to the
     * handler method parameter name
     *
     * @param ex      the validation exception raised for handler method parameters
     * @param headers the HTTP headers to include in the response
     * @param status  the HTTP status computed by Spring
     * @param request the current web request
     * @return a {@link ResponseEntity} containing the {@link ProblemDetail} body
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            @NonNull HandlerMethodValidationException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        HttpServletRequest servletReq = servletRequest(request);

        List<ParameterError> parameterErrors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String parameterName = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(err -> {
                String code = firstCodeOrDefault(err.getCodes());
                parameterErrors.add(new ParameterError(
                        mapValidationCode(code),
                        err.getDefaultMessage(),
                        parameterName,
                        null
                ));
            });
        });

        ValidationDetails details = new ValidationDetails(List.of(), List.of(), parameterErrors);

        String message = "Validation failed. Error count: %d".formatted(parameterErrors.size());

        ProblemDetail pd = problem(
                HttpStatus.BAD_REQUEST,
                ErrorCodes.VALIDATION_FAILED,
                message,
                details,
                servletReq
        );

        return ResponseEntity.badRequest().headers(headers).body(pd);
    }

    /**
     * Handles constraint violations typically raised from service-layer validation.
     *
     * <p>Produces {@code fieldErrors} when the constraint path appears to target a property path.
     *
     * @param ex  the validation exception
     * @param req the current HTTP request
     * @return a {@link ProblemDetail} representing the validation failure
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(@NonNull ConstraintViolationException ex, HttpServletRequest req) {
        List<FieldErrorDetails> fieldErrors = new ArrayList<>();

        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String path = v.getPropertyPath() == null ? null : v.getPropertyPath().toString();
            fieldErrors.add(new FieldErrorDetails(
                    mapValidationCode(v.getConstraintDescriptor() == null ? null : v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()),
                    lastPathSegment(path),
                    v.getMessage(),
                    v.getInvalidValue(),
                    path,
                    path
            ));
        }

        ValidationDetails details = new ValidationDetails(fieldErrors, List.of(), List.of());

        String message = "Validation failed. Error count: %d".formatted(fieldErrors.size());
        return problem(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, message, details, req);
    }

    /**
     * Enriches all {@link ProblemDetail} bodies created by {@link ResponseEntityExceptionHandler}.
     *
     * <p>This hook is invoked for many Spring MVC exceptions that are handled by the base class. If such a response does
     * not already define a {@code code} property, this method assigns:
     * <ul>
     *   <li>{@link ErrorCodes#REQUEST_FAILED} for 4xx responses</li>
     *   <li>{@link ErrorCodes#INTERNAL_ERROR} for 5xx responses</li>
     * </ul>
     *
     * @param body       the response body (possibly {@link ProblemDetail})
     * @param headers    the headers to include
     * @param statusCode the HTTP status
     * @param request    the current web request
     * @return the response entity produced by the base class
     */
    @Override
    @NullMarked
    protected ResponseEntity<Object> createResponseEntity(
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        if (body instanceof ProblemDetail pd && (pd.getProperties() == null || !pd.getProperties().containsKey("code"))) {
            String code = HttpStatus.valueOf(statusCode.value()).is4xxClientError()
                    ? ErrorCodes.REQUEST_FAILED
                    : ErrorCodes.INTERNAL_ERROR;
            ProblemDetailsFactory.enrich(pd, code, servletRequest(request));
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    /**
     * Handles unexpected exceptions not otherwise mapped.
     *
     * <p>Returns a generic message to avoid leaking implementation details. The full exception is logged server-side.
     *
     * @param ex  the unhandled exception
     * @param req the current HTTP request
     * @return a {@link ProblemDetail} for an internal server error
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleFallback(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception path={}", req.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, "Unexpected server error", null, req);
    }

    /**
     * Extracts the {@link HttpServletRequest} from a {@link WebRequest}.
     *
     * @param request the current web request
     * @return the servlet request
     * @throws IllegalStateException if the request is not a {@link ServletWebRequest}
     */
    private @NonNull HttpServletRequest servletRequest(@NonNull WebRequest request) {
        if (request instanceof ServletWebRequest swr) return swr.getRequest();
        throw new IllegalStateException("Expected ServletWebRequest");
    }

    /**
     * Converts Spring's {@link FieldError} to a structured error record.
     *
     * @param fe the Spring field error
     * @return a structured field error
     */
    private FieldErrorDetails toFieldErrorDetails(@NonNull FieldError fe) {
        String rawCode = firstCodeOrDefault(fe.getCodes());
        String message = Optional.ofNullable(fe.getDefaultMessage()).orElse("Invalid value");
        return new FieldErrorDetails(
                mapValidationCode(rawCode),
                fe.getField(),
                message,
                fe.getRejectedValue(),
                fe.getField(),
                fe.getField()
        );
    }

    /**
     * Converts a list of {@link FieldError} to structured field error details.
     *
     * @param fieldErrors the Spring field errors
     * @return a structured list of field errors
     */
    private List<FieldErrorDetails> toFieldErrors(@NonNull List<FieldError> fieldErrors) {
        return fieldErrors.stream().map(this::toFieldErrorDetails).toList();
    }

    /**
     * Converts Spring's object-level errors to structured global errors.
     *
     * @param globalErrors the global errors from a binding result
     * @return a list of structured global errors
     */
    private List<GlobalErrorDetails> toGlobalErrors(@NonNull List<org.springframework.validation.ObjectError> globalErrors) {
        return globalErrors.stream().map(err -> {
            String rawCode = firstCodeOrDefault(err.getCodes());
            String message = Optional.ofNullable(err.getDefaultMessage()).orElse("Invalid value");
            return new GlobalErrorDetails(mapValidationCode(rawCode), message);
        }).toList();
    }

    /**
     * Returns the first message code from an array or the default `"VALIDATION_ERROR"` value.
     *
     * @param codes message codes
     * @return the first code or the fallback
     */
    private String firstCodeOrDefault(@Nullable String[] codes) {
        if (codes == null || codes.length == 0) return "VALIDATION_ERROR";
        return Optional.ofNullable(codes[0]).orElse("VALIDATION_ERROR");
    }

    /**
     * Maps framework/constraint message codes to stable, API-facing validation codes.
     *
     * <p>This mapping is intentionally conservative: it normalizes common validation constraints to your preferred codes.
     * Anything unknown falls back to the raw code uppercased.
     *
     * @param rawCode a raw Spring/Bean Validation code, may be {@code null}
     * @return a stable validation code suitable for API responses
     */
    private String mapValidationCode(@Nullable String rawCode) {
        if (rawCode == null || rawCode.isBlank()) return "VALIDATION_ERROR";
        String c = rawCode;

        // Spring codes often look like "NotBlank.exampleRequestBody.name", "Size.exampleRequestBody.name", etc.
        int dot = c.indexOf('.');
        if (dot > 0) c = c.substring(0, dot);

        return switch (c) {
            case "NotNull" -> "REQUIRED_NOT_NULL";
            case "NotBlank" -> "REQUIRED_NOT_BLANK";
            case "NotEmpty" -> "REQUIRED_NOT_EMPTY";
            case "Size" -> "INVALID_SIZE";
            case "Min" -> "INVALID_MIN";
            case "Max" -> "INVALID_MAX";
            case "Email" -> "INVALID_EMAIL";
            case "Pattern" -> "INVALID_PATTERN";
            default -> c.toUpperCase(Locale.ROOT);
        };
    }

    /**
     * Returns the last segment of a dotted property path.
     *
     * @param path a dotted path
     * @return the last segment, or {@code null} if input is {@code null}
     */
    private @Nullable String lastPathSegment(@Nullable String path) {
        if (path == null) return null;
        int idx = path.lastIndexOf('.');
        return idx >= 0 ? path.substring(idx + 1) : path;
    }

    /**
     * Validation details payload attached to {@link ProblemDetail#getProperties()} under the {@code details} key.
     *
     * @param fieldErrors     field-level validation failures (e.g. {@code @NotBlank} on DTO fields)
     * @param globalErrors    object-level validation failures (e.g. custom {@code @ValidSomething} constraints)
     * @param parameterErrors handler method parameter validation failures (e.g. {@code @RequestParam} constraints)
     */
    public record ValidationDetails(
            List<FieldErrorDetails> fieldErrors,
            List<GlobalErrorDetails> globalErrors,
            List<ParameterError> parameterErrors
    ) {
    }

    /**
     * Detailed field error information, intended to mirror common REST error formats.
     *
     * @param code          stable error code (e.g. {@code REQUIRED_NOT_BLANK}, {@code INVALID_SIZE})
     * @param property      the failing property name (typically the field name)
     * @param message       human-friendly validation message
     * @param rejectedValue the rejected value
     * @param path          the property path (string representation)
     * @param propertyPath  alias for {@code path}; kept for compatibility if you need both
     */
    public record FieldErrorDetails(
            String code,
            @Nullable String property,
            String message,
            @Nullable Object rejectedValue,
            @Nullable String path,
            @Nullable String propertyPath
    ) {
    }

    /**
     * Global (object-level) validation error information.
     *
     * @param code    stable error code (typically derived from constraint/simple name)
     * @param message human-friendly validation message
     */
    public record GlobalErrorDetails(String code, String message) {
    }

    /**
     * Handler method parameter validation error information.
     *
     * @param code          stable error code
     * @param message       human-friendly validation message
     * @param parameter     handler method parameter name
     * @param rejectedValue rejected value if available
     */
    public record ParameterError(
            String code,
            String message,
            @Nullable String parameter,
            @Nullable Object rejectedValue
    ) {
    }
}
