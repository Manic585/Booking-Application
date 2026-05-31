package com.bookingsystem.user.service;

import com.bookingsystem.user.domain.User;
import com.bookingsystem.user.dto.AuthResponse;
import com.bookingsystem.user.dto.LoginRequest;
import com.bookingsystem.user.dto.RegisterRequest;
import com.bookingsystem.user.exception.ConflictException;
import com.bookingsystem.user.exception.UnauthorizedException;
import com.bookingsystem.user.repository.UserRepository;
import com.bookingsystem.user.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String REFRESH_TOKEN_PREFIX = "refresh:";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(User.Role.USER)
                .build();

        userRepository.save(user);
        log.info("New user registered: userId={}, email={}", user.getId(), user.getEmail());

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for email={}", request.getEmail());
            throw new UnauthorizedException("Invalid credentials");
        }

        log.info("User logged in: userId={}", user.getId());
        return buildAuthResponse(user);
    }

    public AuthResponse refresh(String refreshToken) {
        String redisKey = REFRESH_TOKEN_PREFIX + refreshToken;
        String userId = redisTemplate.opsForValue().get(redisKey);

        if (userId == null) {
            throw new UnauthorizedException("Refresh token expired or invalid");
        }

        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException("User not found or inactive"));

        // Rotate the refresh token on each use (prevents replay attacks)
        redisTemplate.delete(redisKey);

        return buildAuthResponse(user);
    }

    public void logout(String refreshToken) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + refreshToken);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // Store refresh token in Redis with 7-day TTL
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + refreshToken,
                user.getId().toString(),
                Duration.ofDays(7)
        );

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpiryMs() / 1000)
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
