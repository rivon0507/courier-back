package io.github.rivon0507.courier.auth.service;

import io.github.rivon0507.courier.auth.domain.RefreshToken;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface RefreshTokenMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "revokedAt", ignore = true)
    @Mapping(target = "revokeReason", ignore = true)
    @Mapping(target = "replacedByTokenId", ignore = true)
    @Mapping(target = "createdByUserAgent", ignore = true)
    @Mapping(target = "createdByIp", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    RefreshToken from(Long userId, UUID familyId, UUID deviceId, byte[] tokenHash, Instant expiresAt);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tokenHash", source = "tokenHash")
    @Mapping(target = "revokedAt", ignore = true)
    @Mapping(target = "revokeReason", ignore = true)
    @Mapping(target = "replacedByTokenId", ignore = true)
    @Mapping(target = "createdByUserAgent", ignore = true)
    @Mapping(target = "createdByIp", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    RefreshToken fromSiblingTokenAndHash(RefreshToken sibling, byte[] tokenHash);
}
