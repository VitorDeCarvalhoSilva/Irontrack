package com.irontrack.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IrontrackApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(IrontrackApiApplication.class, args);
    }
}
