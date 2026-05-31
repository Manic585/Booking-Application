package com.bookingsystem.user;

import com.bookingsystem.user.dto.LoginRequest;
import com.bookingsystem.user.dto.RegisterRequest;
import com.bookingsystem.user.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class AuthServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Use embedded Redis mock or skip Redis for unit focus
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
    }

    @Autowired
    AuthService authService;

    @Test
    void registerAndLogin_shouldSucceed() {
        var req = new RegisterRequest();
        req.setEmail("test@example.com");
        req.setPassword("SecurePass123!");
        req.setFirstName("John");
        req.setLastName("Doe");

        var response = authService.register(req);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getEmail()).isEqualTo("test@example.com");

        var loginReq = new LoginRequest();
        loginReq.setEmail("test@example.com");
        loginReq.setPassword("SecurePass123!");

        var loginResponse = authService.login(loginReq);
        assertThat(loginResponse.getAccessToken()).isNotBlank();
    }

    @Test
    void register_duplicateEmail_shouldThrowConflict() {
        var req = new RegisterRequest();
        req.setEmail("dup@example.com");
        req.setPassword("SecurePass123!");
        req.setFirstName("Jane");
        req.setLastName("Doe");

        authService.register(req);

        assertThatThrownBy(() -> authService.register(req))
                .hasMessageContaining("Email already registered");
    }
}
