package com.example.customermanagementservice.security;

import com.example.customermanagementservice.model.AppUser;
import com.example.customermanagementservice.model.Role;
import com.example.customermanagementservice.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountInitializerTest {

    private static final String PASSWORD = "admin12345";

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Mock
    private AppUserRepository userRepository;

    private AdminAccountInitializer initializer() {
        AppSecurityProperties properties = new AppSecurityProperties(
                new AppSecurityProperties.Jwt("x".repeat(32), Duration.ofHours(1)),
                new AppSecurityProperties.Admin("admin", PASSWORD),
                new AppSecurityProperties.Demo(true, "demo"));
        return new AdminAccountInitializer(userRepository, passwordEncoder, properties);
    }

    @Test
    void createsTheAdmin_withHashedPassword_whenMissing() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());

        initializer().ensureAdminAccount();

        ArgumentCaptor<AppUser> saved = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(saved.capture());
        assertEquals("admin", saved.getValue().getUsername());
        assertEquals(Role.ADMIN, saved.getValue().getRole());
        assertNotEquals(PASSWORD, saved.getValue().getPasswordHash());
        assertTrue(passwordEncoder.matches(PASSWORD, saved.getValue().getPasswordHash()));
    }

    @Test
    void doesNothing_whenTheAdminAlreadyHasThatPassword() {
        AppUser existing = new AppUser("admin", passwordEncoder.encode(PASSWORD), Role.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));

        initializer().ensureAdminAccount();

        verify(userRepository, never()).save(any());
    }

    @Test
    void updatesThePassword_whenTheSettingChanged() {
        AppUser existing = new AppUser("admin", passwordEncoder.encode("old-password"), Role.ADMIN);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));

        initializer().ensureAdminAccount();

        verify(userRepository).save(existing);
        assertTrue(passwordEncoder.matches(PASSWORD, existing.getPasswordHash()));
    }
}
