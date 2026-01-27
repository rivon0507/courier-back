package io.github.rivon0507.courier.auth;

import io.github.rivon0507.courier.auth.api.AuthenticationResponse;
import io.github.rivon0507.courier.auth.api.UserDto;
import io.github.rivon0507.courier.security.AppUserPrincipal;
import io.github.rivon0507.courier.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@EnableConfigurationProperties({JwtProperties.class})
class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final Clock clock = Clock.systemUTC();
    private final JwtProperties jwtProperties;

    public AuthenticationResponse login(@NonNull String username, @NonNull String password) {
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
        long expiresInSeconds = Duration.between(Instant.now(clock), jwt.getExpiresAt()).getSeconds();
        return new AuthenticationResponse(
                jwt.getTokenValue(),
                "Bearer",
                expiresInSeconds,
                new UserDto(principal.email(), principal.displayName(), principal.role())

        );
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
                .claim("name", principal.displayName());

        return jwtEncoder.encode(JwtEncoderParameters.from(claims.build()));
    }
}
