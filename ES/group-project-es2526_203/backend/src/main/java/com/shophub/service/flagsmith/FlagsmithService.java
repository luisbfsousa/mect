package com.shophub.service.flagsmith;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.shophub.config.FlagsmithProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
@ConditionalOnProperty(prefix = "flagsmith", name = "enabled", havingValue = "true")
public class FlagsmithService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlagsmithService.class);
    private static final String FLAG_ENDPOINT = "/flags/";
    private final FlagsmithProperties properties;
    private final RestTemplate restTemplate;

    public FlagsmithService(FlagsmithProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
            .additionalMessageConverters(new MappingJackson2HttpMessageConverter())
            .additionalInterceptors(new EnvironmentKeyInterceptor(properties.getEnvironmentKey()))
            .rootUri(normaliseBaseUrl(properties.getApiUrl()))
            .setConnectTimeout(properties.getTimeout())
            .setReadTimeout(properties.getTimeout())
            .build();
    }

    public Map<String, FeatureFlag> getEnvironmentFlags() {
        if (!properties.isConfigured()) {
            LOGGER.debug("Flagsmith integration not configured, skipping environment flag retrieval");
            return Collections.emptyMap();
        }

        try {
            ResponseEntity<List<FlagsmithFlagResponse>> response = restTemplate.exchange(
                FLAG_ENDPOINT,
                HttpMethod.GET,
                null,
                FlagsmithFlagResponse.RESPONSE_TYPE
            );

            List<FlagsmithFlagResponse> body = response.getBody();
            if (CollectionUtils.isEmpty(body)) {
                return Collections.emptyMap();
            }

            return body.stream()
                .filter(flag -> StringUtils.hasText(flag.getFeatureName()))
                .collect(Collectors.toMap(
                    FlagsmithFlagResponse::getFeatureName,
                    flag -> new FeatureFlag(flag.getFeatureName(), flag.isEnabled(), flag.getValue()),
                    (left, right) -> right,
                    LinkedHashMap::new
                ));
        } catch (Exception ex) {
            LOGGER.warn("Failed to load Flagsmith environment flags: {}", ex.getMessage());
            LOGGER.debug("Flagsmith error details", ex);
            return Collections.emptyMap();
        }
    }

    public Optional<FeatureFlag> getFlag(String featureName) {
        if (!StringUtils.hasText(featureName)) {
            return Optional.empty();
        }

        Map<String, FeatureFlag> flags = getEnvironmentFlags();
        return Optional.ofNullable(flags.get(featureName));
    }

    private String normaliseBaseUrl(@Nullable String apiUrl) {
        if (!StringUtils.hasText(apiUrl)) {
            return "https://edge.api.flagsmith.com/api/v1";
        }
        return apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
    }

    private static class EnvironmentKeyInterceptor implements ClientHttpRequestInterceptor {

        private final String environmentKey;

        private EnvironmentKeyInterceptor(@Nullable String environmentKey) {
            this.environmentKey = environmentKey;
        }

        @Override
        public ClientHttpResponse intercept(
            org.springframework.http.HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
        ) throws java.io.IOException {

            if (StringUtils.hasText(environmentKey)) {
                request.getHeaders().set("X-Environment-Key", environmentKey);
            }
            return execution.execute(request, body);
        }
    }
}
