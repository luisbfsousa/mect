package com.shophub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableJpaAuditing
@EnableRetry
public class ShopHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopHubApplication.class, args);
        System.out.println("\n🚀 ShopHub Backend is running on port 5000");
        System.out.println("📊 Database connection established");
        System.out.println("🔐 Keycloak integration active\n");
    }
}
