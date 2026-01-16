package com.shophub.service.flagsmith;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.core.ParameterizedTypeReference;

@JsonIgnoreProperties(ignoreUnknown = true)
class FlagsmithFlagResponse {

    static final ParameterizedTypeReference<List<FlagsmithFlagResponse>> RESPONSE_TYPE =
        new ParameterizedTypeReference<>() {};

    private FlagFeature feature;
    private boolean enabled;

    @JsonProperty("feature_state_value")
    private Object value;

    public String getFeatureName() {
        return feature != null ? feature.getName() : null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Object getValue() {
        return value;
    }

    public void setFeature(FlagFeature feature) {
        this.feature = feature;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class FlagFeature {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
