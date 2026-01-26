package io.github.rivon0507.courier.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter) {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/_security/ping").authenticated()
                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter))
                )
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    @ConditionalOnExpression("!'${app.security.jwt.public-key-uri}'.isBlank()")
    public JwtDecoder jwtDecoder(JwtProperties props, ResourceLoader resourceLoader) {
        RSAPublicKey publicKey = readRsaPublicKey(props.publicKeyUri(), resourceLoader);
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Bean
    @ConditionalOnExpression("!'${app.security.jwt.public-key-uri}'.isBlank() && !'${app.security.jwt.private-key-uri}'.isBlank()")
    public JwtEncoder jwtEncoder(JwtProperties props, ResourceLoader resourceLoader) {
        RSAPublicKey publicKey = readRsaPublicKey(props.publicKeyUri(), resourceLoader);
        RSAPrivateKey privateKey = readRsaPrivateKey(props.privateKeyUri(), resourceLoader);

        JWK jwk = new RSAKey.Builder(publicKey).privateKey(privateKey).keyID("courier-dev").build();
        JWKSet jwkSet = new JWKSet(jwk);
        ImmutableJWKSet<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();

        return jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            Object rolesClaim = jwt.getClaims().get("roles");
            if (rolesClaim instanceof Collection<?> roles) {
                for (Object r : roles) {
                    if (r != null) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + r));
                    }
                }
            }

            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        if (corsProperties.allowedOrigins().isEmpty())
            throw new IllegalStateException("No CORS allowed origins configured");

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(corsProperties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false); // Set to true when using cookies

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    private static RSAPublicKey readRsaPublicKey(String location, ResourceLoader loader) {
        Resource resource = loader.getResource(location);
        try {
            String pem = resource.getContentAsString(StandardCharsets.UTF_8);
            RSAKey rsaKey = RSAKey.parseFromPEMEncodedObjects(pem).toRSAKey();
            return rsaKey.toRSAPublicKey();
        } catch (IOException | JOSEException e) {
            throw new IllegalStateException("Failed reading RSA public key from " + location, e);
        }
    }

    private static RSAPrivateKey readRsaPrivateKey(String location, ResourceLoader loader) {
        Resource resource = loader.getResource(location);
        try {
            String pem = resource.getContentAsString(StandardCharsets.UTF_8);
            RSAKey rsaKey = RSAKey.parseFromPEMEncodedObjects(pem).toRSAKey();
            return rsaKey.toRSAPrivateKey();
        } catch (IOException | JOSEException e) {
            throw new IllegalStateException("Failed loading RSA private key from " + location, e);
        }
    }
}
