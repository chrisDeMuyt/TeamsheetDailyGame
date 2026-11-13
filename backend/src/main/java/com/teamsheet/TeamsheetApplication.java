package com.teamsheet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TeamsheetApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeamsheetApplication.class, args);
    }
}
