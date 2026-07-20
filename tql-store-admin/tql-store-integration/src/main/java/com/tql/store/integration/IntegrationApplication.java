package com.tql.store.integration;

import com.tql.store.common.web.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableConfigurationProperties({
        com.tql.store.integration.hualala.HualalaProperties.class,
        com.tql.store.integration.hrbutler.HrButlerProperties.class
})
@Import(GlobalExceptionHandler.class)
@SpringBootApplication
public class IntegrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntegrationApplication.class, args);
    }
}
