package io.github.rivon0507.courier.auth.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
public class RefreshToken {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * SHA-256 hash of the raw refresh token.
     */
    @Column(name = "token_hash", nullable = false, unique = true)
    private byte[] tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoke_reason")
    private RevokeReason revokeReason;

    @Column(name = "replaced_by_token_id")
    private UUID replacedByTokenId;

    @Column(name = "created_by_ip")
    private InetAddress createdByIp;

    @Column(name = "created_by_user_agent")
    private String createdByUserAgent;

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now) || expiresAt.equals(now);
    }

    public boolean isActive(Instant now) {
        return revokedAt == null && !isExpired(now);
    }

    public boolean wasRotated() {
        return revokeReason == RevokeReason.ROTATED;
    }

    public boolean wasLoggedOut() {
        return revokeReason == RevokeReason.LOGOUT;
    }

    public boolean wasReused() {
        return revokeReason == RevokeReason.REUSE_DETECTED;
    }

    public void revokeAsRotated(Instant now, UUID replacedByTokenId) {
        this.revokedAt = now;
        this.revokeReason = RevokeReason.ROTATED;
        this.replacedByTokenId = replacedByTokenId;
    }

    public void revokeAsLogout(Instant now) {
        this.revokedAt = now;
        this.revokeReason = RevokeReason.LOGOUT;
    }

    public enum RevokeReason {
        ROTATED,
        LOGOUT,
        REUSE_DETECTED,
        ADMIN
    }
}

