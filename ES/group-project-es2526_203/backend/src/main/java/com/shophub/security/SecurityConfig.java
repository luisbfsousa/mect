package com.shophub.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("!test")
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/health").permitAll()
                .requestMatchers("/api/products", "/api/products/**").permitAll()
                .requestMatchers("/api/categories").permitAll()
                .requestMatchers("/api/reviews/**").permitAll()
                .requestMatchers("/api/chatbot/**").permitAll()
                .requestMatchers("/api/analytics/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                
                // PUBLIC LANDING PAGES AND BANNERS
                .requestMatchers("/api/v1/landing-pages/published/**").permitAll()
                .requestMatchers("/api/v1/banners/active").permitAll()
                
                // PUBLIC BLOG ENDPOINTS - MUST BE BEFORE .anyRequest()
                .requestMatchers("/api/blog/posts", "/api/blog/posts/**").permitAll()
                
                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );
        
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }
    
    // Custom converter to extract roles from Keycloak token
    static class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            
            // Extract realm_access roles
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.get("roles") != null) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) realmAccess.get("roles");
                // Normalize role names: Keycloak often uses hyphens (e.g. content-manager)
                // while Spring Security tests / checks may expect underscores (content_manager).
                // Add authorities for both raw and normalized names to avoid AccessDenied due to naming differences.
                Set<String> uniqueRoles = roles.stream()
                    .flatMap(r -> {
                        String normalized = r.replace('-', '_');
                        return Arrays.stream(new String[]{r, normalized});
                    })
                    .collect(Collectors.toSet());

                authorities.addAll(uniqueRoles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList()));
            }
            
            // Also extract resource_access roles (client-specific roles)
            Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
            if (resourceAccess != null) {
                // Iterate through all clients
                resourceAccess.forEach((client, clientRoles) -> {
                    if (clientRoles instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> clientMap = (Map<String, Object>) clientRoles;
                        Object rolesObj = clientMap.get("roles");
                            if (rolesObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<String> roles = (List<String>) rolesObj;
                                Set<String> uniqueRoles = roles.stream()
                                    .flatMap(r -> {
                                        String normalized = r.replace('-', '_');
                                        return Arrays.stream(new String[]{r, normalized});
                                    })
                                    .collect(Collectors.toSet());

                                authorities.addAll(uniqueRoles.stream()
                                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                                    .collect(Collectors.toList()));
                            }
                    }
                });
            }
            
            System.out.println("🔐 Extracted authorities: " + authorities);
            return authorities;
        }
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        String originsEnv = System.getenv("CORS_ALLOWED_ORIGINS");
        List<String> allowedOrigins = originsEnv != null && !originsEnv.isBlank()
            ? Arrays.stream(originsEnv.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList()
            : List.of("http://localhost:3000", "http://localhost:3001", "http://localhost");

        // Strip any wildcard entries; if nothing remains, fall back to "*"
        List<String> sanitizedOrigins = allowedOrigins.stream()
            .filter(o -> !o.equals("*") && !o.equals("\"*\""))
            .toList();

        if (sanitizedOrigins.isEmpty()) {
            configuration.setAllowedOriginPatterns(List.of("*"));
            // When using wildcard, credentials must be disabled
            configuration.setAllowCredentials(false);
        } else {
            configuration.setAllowedOriginPatterns(sanitizedOrigins);
            // Enable credentials for specific origins
            configuration.setAllowCredentials(true);
        }
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
