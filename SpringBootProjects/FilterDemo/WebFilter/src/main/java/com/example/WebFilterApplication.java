package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@ServletComponentScan(basePackages = "com.example.filter")
@SpringBootApplication
public class WebFilterApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebFilterApplication.class, args);
    }

}
