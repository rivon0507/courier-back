package io.github.rivon0507.courier.security.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for CORS (Cross-Origin Resource Sharing).
 *
 * @param allowedOrigins list of origins permitted to make cross-origin requests
 */
@ConfigurationProperties(prefix = "app.security.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
