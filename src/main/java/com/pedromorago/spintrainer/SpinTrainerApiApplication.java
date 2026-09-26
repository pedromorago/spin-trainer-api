package com.pedromorago.spintrainer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpinTrainerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpinTrainerApiApplication.class, args);
    }
}
