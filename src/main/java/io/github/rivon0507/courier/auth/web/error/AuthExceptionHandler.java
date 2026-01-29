package io.github.rivon0507.courier.auth.web.error;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static io.github.rivon0507.courier.common.web.error.ProblemDetailsFactory.problem;

/**
 * Auth-specific exception handler.
 *
 * <p>This handler is responsible <strong>only</strong> for authentication/session errors
 * that require cookie invalidation (refresh_token and/or device_id).
 *
 * <p>It intentionally does <em>not</em> handle {@link UnauthorizedException} in general:
 * those are delegated to the global exception handler since failed login attempts do
 * not involve clearing cookies.
 */
@RestControllerAdvice
@Slf4j
public class AuthExceptionHandler {

    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String DEVICE_COOKIE = "device_id";

    @ExceptionHandler(InvalidSessionException.class)
    public ResponseEntity<ProblemDetail> handleInvalidSession(
            InvalidSessionException ex,
            HttpServletRequest req
    ) {
        log.warn("Invalid session path={} message={}", req.getRequestURI(), ex.getReason());
        ProblemDetail pd = problem(ex.status(), ex.code(), ex.getMessage(), null, req);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, clearCookie(REFRESH_COOKIE).toString());

        return ResponseEntity.status(ex.status()).headers(headers).body(pd);
    }

    @ExceptionHandler(InvalidDeviceIdException.class)
    public ResponseEntity<ProblemDetail> handleInvalidDeviceId(
            InvalidDeviceIdException ex,
            HttpServletRequest req
    ) {
        log.warn("Invalid device_id cookie path={} message={}", req.getRequestURI(), ex.getReason());
        ProblemDetail pd = problem(ex.status(), ex.code(), ex.getMessage(), null, req);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, clearCookie(REFRESH_COOKIE).toString());
        headers.add(HttpHeaders.SET_COOKIE, clearCookie(DEVICE_COOKIE).toString());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).headers(headers).body(pd);
    }

    private ResponseCookie clearCookie(@NonNull String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .build();
    }
}
