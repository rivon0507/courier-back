package io.github.rivon0507.courier.auth.service;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class RefreshTokenHasher {
    public byte[] hash(@NonNull String rawToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(rawToken.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
