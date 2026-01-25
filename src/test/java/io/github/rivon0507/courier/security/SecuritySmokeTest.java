package io.github.rivon0507.courier.security;

import io.github.rivon0507.courier.TestJwtConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"""
                        spring.autoconfigure.exclude=\
                        org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,\
                        org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,\
                        org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration
        """})
@AutoConfigureMockMvc
@Import(TestJwtConfiguration.class)
@ActiveProfiles("test")
public class SecuritySmokeTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtEncoder jwtEncoder;

    @Test
    void unauthenticatedRequest__returns401() throws Exception {
        mockMvc.perform(get("/random"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedRequest__returns200() throws Exception {
        String token = token(Set.of("user"));
        mockMvc.perform(get("/_security/ping")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequest_toUnguardedEndpoint__returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    String token(Set<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("courier")
                .subject("test-user")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("roles", roles)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
