package com.shophub.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("test")
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - match production SecurityConfig
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/products", "/api/products/**").permitAll()
                .requestMatchers("/api/categories").permitAll()
                .requestMatchers("/api/reviews/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/blog/posts", "/api/blog/posts/**").permitAll()
                .requestMatchers("/api/v1/landing-pages", "/api/v1/landing-pages/**").permitAll()
                .requestMatchers("/api/v1/banners", "/api/v1/banners/**").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            );
        
        return http.build();
    }
}
