package io.github.rivon0507.courier.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtPropertiesValidator implements InitializingBean {

    private final JwtProperties props;
    private final Environment environment;

    @Override
    public void afterPropertiesSet() {
        boolean allowClasspath = environment.matchesProfiles("test && !prod");
        if (!allowClasspath) {
            String publicKeyUri = props.publicKeyUri();
            if (publicKeyUri != null && publicKeyUri.startsWith("classpath:"))
                throw new IllegalStateException("public key" + " URI must not be a classpath resource in production");
            String privateKeyUri = props.privateKeyUri();
            if (privateKeyUri != null && privateKeyUri.startsWith("classpath:"))
                throw new IllegalStateException("private key" + " URI must not be a classpath resource in production");
        }
    }
}