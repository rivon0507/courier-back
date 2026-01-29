package io.github.rivon0507.courier.auth.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(byte[] tokenHash);

    List<RefreshToken> findAllByFamilyId(UUID familyId);

    @Modifying
    @Query("""
            update RefreshToken t
            set t.revokedAt = :now,
                t.revokeReason = :reason,
                t.replacedByTokenId = null
            where t.deviceId = :deviceId
              and t.revokedAt is null
            """)
    void revokeActiveByDeviceId(UUID deviceId, Instant now, RefreshToken.RevokeReason reason);

    @Modifying
    @Query("""
            update RefreshToken t
            set t.revokedAt = :now,
                t.revokeReason = :reason,
                t.replacedByTokenId = null
            where t.familyId = :familyId
              and t.revokedAt is null
            """)
    void revokeActiveByFamilyId(UUID familyId, Instant now, RefreshToken.RevokeReason reason);
}
