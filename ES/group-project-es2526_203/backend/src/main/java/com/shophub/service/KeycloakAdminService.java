package com.shophub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    @Value("${keycloak.url:http://localhost:8080}")
    private String keycloakUrl;

    @Value("${keycloak.realm:ShopHub}")
    private String realm;

    // Supports either property keycloak.admin-username or env KEYCLOAK_ADMIN_USERNAME
    @Value("${keycloak.admin-username:${KEYCLOAK_ADMIN_USERNAME:}}")
    private String adminUsername;

    // Supports either property keycloak.admin-password or env KEYCLOAK_ADMIN_PASSWORD
    @Value("${keycloak.admin-password:${KEYCLOAK_ADMIN_PASSWORD:}}")
    private String adminPassword;

    private final RestTemplate restTemplate = new RestTemplate();

    private String getAdminAccessToken() {
        if (adminUsername == null || adminUsername.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.debug("Keycloak admin credentials not configured; skipping admin sync");
            return null;
        }
        String tokenUrl = keycloakUrl + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", "admin-cli");
        form.add("username", adminUsername);
        form.add("password", adminPassword);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(tokenUrl, req, Map.class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Failed to obtain Keycloak admin token: status=" + resp.getStatusCode());
        }
        Object token = resp.getBody().get("access_token");
        if (token == null) {
            throw new IllegalStateException("Keycloak token response missing access_token");
        }
        return token.toString();
    }

    @Transactional(readOnly = true)
    public void updateUserProfile(String userId, String firstName, String lastName, String email) {
        String token = getAdminAccessToken();
        if (token == null) {
            return; // credentials not configured; skip silently
        }

        String url = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        // Build minimal update payload
        String body = String.format("{\"firstName\":%s,\"lastName\":%s,\"email\":%s}",
                jsonString(firstName), jsonString(lastName), jsonString(email));

        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Void> resp = restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
        if (resp.getStatusCode().is2xxSuccessful()) {
            log.info("Synced user {} to Keycloak (name/email)", userId);
        } else {
            log.warn("Keycloak sync for user {} failed with status {}", userId, resp.getStatusCode());
        }
    }

    private String jsonString(String s) {
        if (s == null) return "null";
        // Simple JSON string escape
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Transactional(readOnly = true)
    public void setUserEnabled(String userId, boolean enabled) {
        String token = getAdminAccessToken();
        if (token == null) return;

        String url = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);

        String body = "{\"enabled\":" + (enabled ? "true" : "false") + "}";
        HttpEntity<String> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Void> resp = restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
        if (resp.getStatusCode().is2xxSuccessful()) {
            log.info("Set Keycloak user {} enabled={} successfully", userId, enabled);
        } else {
            log.warn("Failed to set enabled={} for user {}: status {}", enabled, userId, resp.getStatusCode());
        }
    }

    @Transactional(readOnly = true)
    public void logoutUserSessions(String userId) {
        String token = getAdminAccessToken();
        if (token == null) return;

        String url = keycloakUrl + "/admin/realms/" + realm + "/users/" + userId + "/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<Void> resp = restTemplate.postForEntity(url, entity, Void.class);
        if (resp.getStatusCode().is2xxSuccessful()) {
            log.info("Logged out active sessions for user {}", userId);
        } else {
            log.warn("Failed to logout sessions for user {}: status {}", userId, resp.getStatusCode());
        }
    }
}
