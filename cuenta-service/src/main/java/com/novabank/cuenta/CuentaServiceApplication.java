package com.novabank.cuenta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class CuentaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuentaServiceApplication.class, args);
    }
}
