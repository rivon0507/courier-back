package io.github.rivon0507.courier.auth.service;

import io.github.rivon0507.courier.auth.UserMapper;
import io.github.rivon0507.courier.auth.api.AuthenticationResponse;
import io.github.rivon0507.courier.auth.domain.RefreshToken;
import io.github.rivon0507.courier.auth.domain.RefreshTokenRepository;
import io.github.rivon0507.courier.auth.web.error.EmailAlreadyTakenException;
import io.github.rivon0507.courier.auth.web.error.InvalidDeviceIdException;
import io.github.rivon0507.courier.auth.web.error.InvalidSessionException;
import io.github.rivon0507.courier.auth.web.error.UnauthorizedException;
import io.github.rivon0507.courier.common.domain.Role;
import io.github.rivon0507.courier.common.domain.User;
import io.github.rivon0507.courier.common.domain.Workspace;
import io.github.rivon0507.courier.common.persistence.UserRepository;
import io.github.rivon0507.courier.security.AppUserPrincipal;
import io.github.rivon0507.courier.security.configuration.JwtProperties;
import io.github.rivon0507.courier.security.configuration.SessionProperties;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String UNIQUE_EMAIL_CONSTRAINT = "uk_users_email";
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final Clock clock = Clock.systemUTC();
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final RefreshTokenMapper refreshTokenMapper;
    private final SessionProperties sessionProperties;
    private final SessionRevocationService sessionRevocationService;

    /**
     * Controller should pass the device_id cookie if present. If absent, we create a new one.
     * This method revokes any currently active session(s) for that device_id to prevent "dangling" tokens.
     */
    @Transactional
    public AuthSessionResult login(@NonNull String username, @NonNull String password, @Nullable String deviceId) {
        UUID deviceUuid = ensureDeviceId(deviceId);
        Authentication authenticated;
        try {
            authenticated = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
        } catch (BadCredentialsException | DisabledException e) {
            throw new UnauthorizedException(e);
        }
        Objects.requireNonNull(authenticated.getPrincipal(), "Authenticated principal must not be null");
        AppUserPrincipal principal = (AppUserPrincipal) authenticated.getPrincipal();
        Jwt jwt = encodeAccessToken(principal);

        revokeActiveByDevice(deviceUuid);
        String refreshToken = issueRefreshToken(principal.id(), deviceUuid);
        AuthenticationResponse response = toAuthResponse(jwt, principal);

        return new AuthSessionResult(
                response,
                new AuthSessionResult.RefreshCookies(refreshToken, deviceUuid.toString())
        );
    }

    @Transactional
    public AuthSessionResult register(@NonNull String email,
                                      @NonNull String password,
                                      @NonNull String displayName,
                                      @Nullable String deviceId) {
        UUID deviceUuid = ensureDeviceId(deviceId);
        User user = userMapper.from(email, displayName, Role.USER);
        user.setPasswordHash(passwordEncoder.encode(password));
        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
             if (e.getLocalizedMessage().contains(UNIQUE_EMAIL_CONSTRAINT)) throw new EmailAlreadyTakenException();
            throw e;
        }

        Workspace defaultWorkspace = new Workspace();
        defaultWorkspace.setOwner(saved);
        saved.setDefaultWorkspace(defaultWorkspace);
        userRepository.save(saved);

        AppUserPrincipal principal = userMapper.toUserPrincipal(saved);
        Jwt jwt = encodeAccessToken(principal);

        revokeActiveByDevice(deviceUuid);
        String refreshToken = issueRefreshToken(saved.getId(), deviceUuid);

        return new AuthSessionResult(
                toAuthResponse(jwt, principal),
                new AuthSessionResult.RefreshCookies(refreshToken, deviceUuid.toString())
        );
    }

    /**
     * Refresh endpoint: requires BOTH cookies.
     * <p>
     * - missing device_id => INVALID_SESSION (401)
     * <p>
     * - missing refresh_token => 401
     * <p>
     * - token not found / mismatched device / expired / revoked => 401
     * <p>
     * - revoked+ROTATED => reuse detection => revoke all active tokens in family with REUSE_DETECTED => 401
     */
    @Transactional
    public AuthSessionResult refreshSession(@Nullable String refreshToken, @Nullable String deviceId) {
        if (deviceId == null) throw new InvalidSessionException("device_id is null");
        UUID deviceUuid = parseDeviceId(deviceId);
        if (refreshToken == null) throw new InvalidSessionException("refresh_token is null or blank");

        byte[] tokenHash = refreshTokenHasher.hash(refreshToken);
        RefreshToken t1 = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidSessionException("Refresh token does not exist in DB"));

        if (!t1.getDeviceId().equals(deviceUuid)) throw new InvalidSessionException("device_id did not match the refresh token's");
        Instant now = Instant.now(clock);
        if (t1.isExpired(now)) throw new InvalidSessionException("refresh_token has expired");

        if (t1.getRevokedAt() != null) {
            if (t1.wasRotated() || t1.wasReused()) {
                sessionRevocationService.revokeFamilyOfReusedToken(t1.getFamilyId(), now);
                throw new InvalidSessionException("refresh_token reuse detected", "REFRESH_TOKEN_REUSED");
            }
            throw new InvalidSessionException("refresh_token was revoked");
        }

        String rawToken = UUID.randomUUID().toString();
        RefreshToken token = refreshTokenMapper.fromSiblingTokenAndHash(t1, refreshTokenHasher.hash(rawToken));
        RefreshToken saved = refreshTokenRepository.save(token);

        t1.revokeAsRotated(now, saved.getId());
        refreshTokenRepository.save(t1);

        AppUserPrincipal principal = userRepository.findById(saved.getUserId())
                .map(userMapper::toUserPrincipal)
                .orElseThrow(() -> new InvalidSessionException("user not found for refresh_token"));

        Jwt jwt = encodeAccessToken(principal);
        AuthenticationResponse response = toAuthResponse(jwt, principal);

        return new AuthSessionResult(
                response,
                new AuthSessionResult.RefreshCookies(rawToken, deviceUuid.toString())
        );
    }

    @Transactional
    public void logout(@Nullable String refreshToken, @Nullable String deviceId) {
        if (refreshToken == null || deviceId == null) return;
        UUID deviceUuid;
        try {
            deviceUuid = UUID.fromString(deviceId);
        } catch (IllegalArgumentException e) {
            return;
        }
        var optionalSession = refreshTokenRepository.findByTokenHash(refreshTokenHasher.hash(refreshToken));
        if (optionalSession.isEmpty()) return;
        RefreshToken session = optionalSession.get();
        if (!session.getDeviceId().equals(deviceUuid)) return;
        Instant now = Instant.now(clock);
        if (session.isExpired(now)) return;
        if (session.getRevokedAt() != null) return;
        session.revokeAsLogout(now);
        refreshTokenRepository.save(session);
    }

    private AuthenticationResponse toAuthResponse(Jwt jwt, AppUserPrincipal principal) {
        long expiresInSeconds = Duration.between(Instant.now(clock), jwt.getExpiresAt()).getSeconds();
        return new AuthenticationResponse(
                jwt.getTokenValue(),
                "Bearer",
                expiresInSeconds,
                userMapper.principalToUserDto(principal),
                principal.defaultWorkspaceId()
        );
    }

    /**
     * Parses a UUID from the provided string or generates a random one if the provided string cannot be parsed.
     *
     * @param deviceId the string to parse into a UUID
     * @return the parsed UUID or a random one via {@link  UUID#randomUUID()}
     * @see AuthService#parseDeviceId(String)
     */
    private UUID ensureDeviceId(@Nullable String deviceId) {
        if (deviceId == null) return UUID.randomUUID();
        try {
            return UUID.fromString(deviceId);
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }

    /**
     * Parses a UUID from the provided string.
     * <p>
     * Unlike {@link AuthService#ensureDeviceId(String)}, this method throws if the string cannot be parsed.
     *
     * @param deviceId the string to parse into a UUID
     * @return the parsed UUID or a random one via {@link  UUID#randomUUID()}
     * @throws InvalidDeviceIdException if the device id is malformed
     * @see AuthService#ensureDeviceId(String)
     */
    private UUID parseDeviceId(@NonNull String deviceId) {
        try {
            return UUID.fromString(deviceId);
        } catch (IllegalArgumentException e) {
            throw new InvalidDeviceIdException();
        }
    }

    private void revokeActiveByDevice(UUID deviceId) {
        Instant now = Instant.now(clock);
        refreshTokenRepository.revokeActiveByDeviceId(deviceId, now, RefreshToken.RevokeReason.LOGOUT);
    }

    private String issueRefreshToken(Long userId, UUID deviceId) {
        UUID familyId = UUID.randomUUID();
        Instant expiresAt = Instant.now().plus(sessionProperties.refreshTokenTtl());
        String raw = UUID.randomUUID().toString();
        byte[] hash = refreshTokenHasher.hash(raw);
        RefreshToken token = refreshTokenMapper.from(userId, familyId, deviceId, hash, expiresAt);
        refreshTokenRepository.save(token);
        return raw;
    }

    private Jwt encodeAccessToken(AppUserPrincipal principal) {
        Instant now = Instant.now(clock);
        Instant exp = now.plus(jwtProperties.accessTokenTtl());

        String scope = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .reduce((a, b) -> a + " " + b)
                .orElse("");

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(exp)
                .subject(principal.email())
                .claim("scope", scope)
                .claim("roles", new String[]{principal.role()})
                .claim("name", principal.displayName())
                .claim("userId", principal.id());

        return jwtEncoder.encode(JwtEncoderParameters.from(claims.build()));
    }
}
