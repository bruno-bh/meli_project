package com.meli.productapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class ProductApiApplication {

    public static void main(String[] args) {
        log.info("Starting Product API application...");
        SpringApplication.run(ProductApiApplication.class, args);
    }
}
