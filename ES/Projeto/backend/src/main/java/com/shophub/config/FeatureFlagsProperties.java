package com.shophub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Provides default values for feature toggles when Flagsmith is unavailable
 * or a flag is not defined remotely. Each property can be overridden through
 * application.yml or environment variables (e.g. FEATURES_LANDING_PAGES_ENABLED).
 */
@Component
@ConfigurationProperties(prefix = "features")
public class FeatureFlagsProperties {

    /**
     * Controls content-manager landing pages backend behaviour when no remote flag value exists.
     */
    private boolean landingPagesEnabled = true;

    /**
     * Controls promotional banner management fallback behaviour.
     */
    private boolean promotionalBannersEnabled = true;

    /**
     * Controls theme customisation fallback behaviour.
     */
    private boolean themeCustomizationEnabled = true;

    public boolean isLandingPagesEnabled() {
        return landingPagesEnabled;
    }

    public void setLandingPagesEnabled(boolean landingPagesEnabled) {
        this.landingPagesEnabled = landingPagesEnabled;
    }

    public boolean isPromotionalBannersEnabled() {
        return promotionalBannersEnabled;
    }

    public void setPromotionalBannersEnabled(boolean promotionalBannersEnabled) {
        this.promotionalBannersEnabled = promotionalBannersEnabled;
    }

    public boolean isThemeCustomizationEnabled() {
        return themeCustomizationEnabled;
    }

    public void setThemeCustomizationEnabled(boolean themeCustomizationEnabled) {
        this.themeCustomizationEnabled = themeCustomizationEnabled;
    }
}
