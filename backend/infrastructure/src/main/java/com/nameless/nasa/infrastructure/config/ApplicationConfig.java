package com.nameless.nasa.infrastructure.config;

import com.nameless.nasa.application.annotations.Handler;
import com.nameless.nasa.application.annotations.UseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import java.time.Clock;

@Configuration
@ComponentScan(
        basePackages = "com.nameless.nasa.application",
        useDefaultFilters = false,
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = {UseCase.class, Handler.class}))
public class ApplicationConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
