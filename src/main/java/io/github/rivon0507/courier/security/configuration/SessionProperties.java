package io.github.rivon0507.courier.security.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.security.session")
public record SessionProperties (
        Duration refreshTokenTtl
) {
}
