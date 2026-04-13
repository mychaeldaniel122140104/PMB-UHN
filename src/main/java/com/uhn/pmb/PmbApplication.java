package com.uhn.pmb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PmbApplication {

    public static void main(String[] args) {
        SpringApplication.run(PmbApplication.class, args);
    }

}
