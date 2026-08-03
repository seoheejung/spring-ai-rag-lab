package com.example.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class SpringAiRagLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiRagLabApplication.class, args);
    }

}
