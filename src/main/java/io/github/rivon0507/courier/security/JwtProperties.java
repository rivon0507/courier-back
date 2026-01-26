package io.github.rivon0507.courier.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties holder for JWT-related settings. Bound to properties under the prefix `app.security.jwt`.
 *
 * @param publicKeyUri: location (URI or classpath) of the JWT public key used to verify tokens.
 * @param privateKeyUri: location (URI or classpath) of the JWT private key used to sign tokens.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties (
        String publicKeyUri,
        String privateKeyUri
) {
}
