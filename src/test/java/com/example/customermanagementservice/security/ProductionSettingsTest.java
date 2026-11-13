package com.example.customermanagementservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads the real application.properties / application-prod.properties and checks that production
 * can never start with the public development secrets.
 */
class ProductionSettingsTest {

    @Configuration
    @EnableConfigurationProperties(AppSecurityProperties.class)
    static class SecuritySettings {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(SecuritySettings.class);

    @Test
    void development_usesTheDefaults() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(AppSecurityProperties.class).admin().password()).isEqualTo("admin12345");
        });
    }

    @Test
    void production_withoutSecrets_doesNotStart() {
        runner.withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void production_withoutAdminPassword_doesNotStart() {
        runner.withPropertyValues("spring.profiles.active=prod", "JWT_SECRET=" + "s".repeat(40))
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void production_withSecrets_usesThem() {
        runner.withPropertyValues("spring.profiles.active=prod",
                        "JWT_SECRET=" + "s".repeat(40), "ADMIN_PASSWORD=a-strong-password")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    AppSecurityProperties properties = context.getBean(AppSecurityProperties.class);
                    assertThat(properties.admin().password()).isEqualTo("a-strong-password");
                    assertThat(properties.jwt().secret()).isEqualTo("s".repeat(40));
                });
    }
}
