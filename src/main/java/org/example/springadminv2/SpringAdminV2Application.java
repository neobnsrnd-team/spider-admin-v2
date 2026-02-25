package org.example.springadminv2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SpringAdminV2Application {

    public static void main(String[] args) {
        SpringApplication.run(SpringAdminV2Application.class, args);
    }
}
