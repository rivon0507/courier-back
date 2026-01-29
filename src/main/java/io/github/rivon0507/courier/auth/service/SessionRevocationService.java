package io.github.rivon0507.courier.auth.service;

import io.github.rivon0507.courier.auth.domain.RefreshToken;
import io.github.rivon0507.courier.auth.domain.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionRevocationService {
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    void revokeFamilyOfReusedToken(UUID familyId, Instant now) {
        refreshTokenRepository.revokeActiveByFamilyId(familyId, now, RefreshToken.RevokeReason.REUSE_DETECTED);
    }
}
