package com.nameless.nasa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        System.setProperty("com.sun.security.enableAIAcaIssuers", "true");
        SpringApplication.run(BackendApplication.class, args);
    }

}
