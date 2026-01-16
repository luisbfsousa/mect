package com.shophub.service;

import com.shophub.config.FeatureFlagsProperties;
import com.shophub.service.flagsmith.FeatureFlag;
import com.shophub.service.flagsmith.FlagsmithService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Centralised feature toggle helper that relies on Flagsmith when available.
 */
@Service
public class FeatureToggleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureToggleService.class);

    public static final String FLAG_WAREHOUSE_SHIP_ORDERS = "warehouse_ship_orders";
    public static final String FLAG_WAREHOUSE_DELIVER_ORDERS = "warehouse_deliver_orders";
    public static final String FLAG_CONTENT_MANAGER_LANDING_PAGES = "content_manager_landing_pages";
    public static final String FLAG_PROMOTIONAL_BANNERS = "promotional_banners";
    public static final String FLAG_THEME_CUSTOMIZATION = "theme_customization";

    private final FlagsmithService flagsmithService;
    private final FeatureFlagsProperties defaultFlags;

    public FeatureToggleService(
        @Autowired(required = false) FlagsmithService flagsmithService,
        FeatureFlagsProperties defaultFlags
    ) {
        this.flagsmithService = flagsmithService;
        this.defaultFlags = defaultFlags;
    }

    public boolean isFeatureEnabled(String featureName, boolean fallback) {
        if (!StringUtils.hasText(featureName)) {
            return fallback;
        }

        if (flagsmithService == null) {
            return fallback;
        }

        return flagsmithService.getFlag(featureName)
            .map(FeatureFlag::enabled)
            .orElseGet(() -> {
                LOGGER.debug("Flagsmith returned no value for feature '{}'; falling back to {}", featureName, fallback);
                return fallback;
            });
    }

    public boolean isWarehouseShipOrdersEnabled() {
        return isFeatureEnabled(FLAG_WAREHOUSE_SHIP_ORDERS, true);
    }

    public boolean isWarehouseDeliverOrdersEnabled() {
        return isFeatureEnabled(FLAG_WAREHOUSE_DELIVER_ORDERS, true);
    }

    public boolean isLandingPagesEnabled() {
        return isFeatureEnabled(FLAG_CONTENT_MANAGER_LANDING_PAGES, defaultFlags.isLandingPagesEnabled());
    }

    public boolean isPromotionalBannersEnabled() {
        return isFeatureEnabled(FLAG_PROMOTIONAL_BANNERS, defaultFlags.isPromotionalBannersEnabled());
    }

    public boolean isThemeCustomizationEnabled() {
        return isFeatureEnabled(FLAG_THEME_CUSTOMIZATION, defaultFlags.isThemeCustomizationEnabled());
    }
}
