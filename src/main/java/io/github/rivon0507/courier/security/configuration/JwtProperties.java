package io.github.rivon0507.courier.security.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties holder for JWT-related settings. Bound to properties under the prefix `app.security.jwt`.
 *
 * @param publicKeyUri location (URI or classpath) of the JWT public key used to verify tokens.
 * @param privateKeyUri location (URI or classpath) of the JWT private key used to sign tokens.
 * @param issuer issuer expected `iss` claim value for incoming tokens
 * @param accessTokenTtl JWT access token's time-to-live
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties (
        String publicKeyUri,
        String privateKeyUri,
        String issuer,
        Duration accessTokenTtl
) {
}
