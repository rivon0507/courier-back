package io.github.rivon0507.courier;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.*;

import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@TestConfiguration
public class TestJwtConfiguration {

    @Bean
    public KeyPair testRsaKeyPair() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    @Bean
    @Primary
    public JwtDecoder testJwtDecoder(KeyPair testRsaKeyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) testRsaKeyPair.getPublic();
        return NimbusJwtDecoder.withPublicKey(publicKey).build();
    }

    @Bean
    @Primary
    public JwtEncoder testJwtEncoder(KeyPair testRsaKeyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) testRsaKeyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) testRsaKeyPair.getPrivate();

        JWK jwk = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID("test-kid")
                .build();

        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(jwk)));
    }
}
