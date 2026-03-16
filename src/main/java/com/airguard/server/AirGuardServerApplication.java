package com.airguard.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AirGuardServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AirGuardServerApplication.class, args);
    }

}
