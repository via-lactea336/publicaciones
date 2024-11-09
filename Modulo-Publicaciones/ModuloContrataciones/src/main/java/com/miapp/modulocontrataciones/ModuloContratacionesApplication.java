package com.miapp.modulocontrataciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;

@EntityScan(basePackages = "com.miapp.sistemasdistribuidos.entity")
@SpringBootApplication
@EnableCaching
public class ModuloContratacionesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModuloContratacionesApplication.class, args);
    }
}
