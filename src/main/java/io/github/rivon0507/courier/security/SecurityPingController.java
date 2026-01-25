package io.github.rivon0507.courier.security;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/_security/ping")
class SecurityPingController {

    @GetMapping
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
