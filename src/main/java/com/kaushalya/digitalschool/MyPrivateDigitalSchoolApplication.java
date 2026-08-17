package com.kaushalya.digitalschool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MyPrivateDigitalSchoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyPrivateDigitalSchoolApplication.class, args);
    }
}
