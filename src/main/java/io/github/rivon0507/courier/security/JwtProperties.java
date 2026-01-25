package io.github.rivon0507.courier.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JWT key file locations.
 * <p>
 * Maps properties under {@code app.security.jwt}:
 * - {@code public-key-path} -> {@code publicKeyPath}
 * - {@code private-key-path} -> {@code privateKeyPath}
 *
 * Expected values are paths to PEM-formatted key files (classpath or filesystem)
 * used to verify and sign JSON Web Tokens.
 *
 * @param publicKeyPath path to the PEM public key used to verify JWT signatures (classpath or filesystem)
 * @param privateKeyPath path to the PEM private key used to sign JWTs (classpath or filesystem)
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties (
        String publicKeyPath,
        String privateKeyPath
) {
}
