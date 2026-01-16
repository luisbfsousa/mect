package com.shophub.service.flagsmith;

public record FeatureFlag(String name, boolean enabled, Object value) {
}
