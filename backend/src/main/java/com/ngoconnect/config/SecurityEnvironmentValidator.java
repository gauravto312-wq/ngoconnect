package com.ngoconnect.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Validates that critical security environment variables are supplied
 * in non-test profiles, failing application startup safely if absent.
 */
@Component
@Profile("!test")
public class SecurityEnvironmentValidator {

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Value("${app.jwt.secret:}")
    private String jwtSecret;

    @PostConstruct
    public void validateSecurityConfiguration() {
        if (!StringUtils.hasText(datasourcePassword)) {
            throw new IllegalStateException("CRITICAL CONFIGURATION ERROR: Database password is not set. Please provide SPRING_DATASOURCE_PASSWORD via environment variables.");
        }
        if (!StringUtils.hasText(jwtSecret) || jwtSecret.trim().length() < 32) {
            throw new IllegalStateException("CRITICAL CONFIGURATION ERROR: JWT signing secret is missing or insufficiently secure (minimum 256 bits / 32 characters required). Please provide JWT_SECRET via environment variables.");
        }
    }
}
