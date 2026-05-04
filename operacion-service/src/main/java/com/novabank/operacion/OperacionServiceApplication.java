package com.novabank.operacion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class OperacionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OperacionServiceApplication.class, args);
    }
}
