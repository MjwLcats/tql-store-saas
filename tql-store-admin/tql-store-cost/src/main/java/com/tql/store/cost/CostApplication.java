package com.tql.store.cost;

import com.tql.store.common.web.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(GlobalExceptionHandler.class)
@SpringBootApplication
public class CostApplication {
    public static void main(String[] args) {
        SpringApplication.run(CostApplication.class, args);
    }
}
