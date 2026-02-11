package io.github.rivon0507.courier.security;

import io.github.rivon0507.courier.TestJwtConfiguration;
import io.github.rivon0507.courier.common.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = """
        spring.flyway.enabled=false
        spring.jpa.hibernate.ddl-auto=create-drop
        """)
@AutoConfigureTestDatabase
@AutoConfigureMockMvc
@Import(TestJwtConfiguration.class)
@ActiveProfiles({"test"})
public class SecuritySmokeTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    UserRepository userRepository;

    @Test
    void unauthenticatedRequest__returns401() throws Exception {
        mockMvc.perform(get("/random"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedRequest__returns200() throws Exception {
        mockMvc.perform(get("/_security/ping")
                        .with(jwt().jwt(jwt -> jwt.claim("userId", 1))))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequest_toUnguardedEndpoint__returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
