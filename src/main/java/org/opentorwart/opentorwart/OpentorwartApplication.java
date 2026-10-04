package org.opentorwart.opentorwart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("org.opentorwart.opentorwart.config")
public class OpentorwartApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpentorwartApplication.class, args);
    }

}
