package com.finpay.collections;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CollectionsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CollectionsServiceApplication.class, args);
    }
}
