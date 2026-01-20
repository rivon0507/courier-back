package io.github.rivon0507.courierback;

import org.springframework.boot.SpringApplication;

public class TestCourierBackApplication {

    public static void main(String[] args) {
        SpringApplication.from(CourierBackApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
