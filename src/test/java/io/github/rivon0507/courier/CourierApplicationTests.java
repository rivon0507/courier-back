package io.github.rivon0507.courier;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import({TestcontainersConfiguration.class, TestJwtConfiguration.class})
@SpringBootTest
@ActiveProfiles("test")
class CourierApplicationTests {

    @Test
    void contextLoads() {
    }

}
