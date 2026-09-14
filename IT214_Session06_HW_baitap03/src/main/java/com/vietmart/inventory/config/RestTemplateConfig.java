package com.vietmart.inventory.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Cấu hình RestTemplate có gắn @LoadBalanced và thiết lập Timeout chặt chẽ:
 * - connectTimeout: 1 giây (1000ms)
 * - readTimeout: 2 giây (2000ms)
 * Ngăn chặn tuyệt đối việc worker thread bị block vĩnh viễn gây cạn kiệt thread pool.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(1))
                .setReadTimeout(Duration.ofSeconds(2))
                .build();
    }
}
