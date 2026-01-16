package com.shophub.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {
    
    public String getUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getSubject();
        }
        return null;
    }
    
    public String getEmail(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getClaimAsString("email");
        }
        return null;
    }
    
    public String getFirstName(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String name = jwt.getClaimAsString("given_name");
            if (name == null) {
                String fullName = jwt.getClaimAsString("name");
                if (fullName != null) {
                    return fullName.split(" ")[0];
                }
            }
            return name;
        }
        return null;
    }
    
    public String getLastName(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String name = jwt.getClaimAsString("family_name");
            if (name == null) {
                String fullName = jwt.getClaimAsString("name");
                if (fullName != null) {
                    String[] parts = fullName.split(" ");
                    if (parts.length > 1) {
                        return parts[parts.length - 1];
                    }
                }
            }
            return name;
        }
        return null;
    }
}
