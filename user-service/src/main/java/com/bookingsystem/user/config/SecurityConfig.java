package com.bookingsystem.user.config;

import com.bookingsystem.user.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/actuator/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // Extract user from gateway-propagated headers (X-User-Id, X-User-Role)
    // Downstream services trust the gateway; they don't re-validate JWT.
    @Bean
    public OncePerRequestFilter jwtAuthFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                String userId = request.getHeader("X-User-Id");
                String role = request.getHeader("X-User-Role");

                if (userId != null && role != null) {
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    // Direct call without gateway — validate JWT for local dev/testing
                    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        try {
                            String token = authHeader.substring(7);
                            Claims claims = jwtTokenProvider.parseToken(token);
                            String subject = claims.getSubject();
                            String claimRole = claims.get("role", String.class);
                            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claimRole));
                            SecurityContextHolder.getContext().setAuthentication(
                                    new UsernamePasswordAuthenticationToken(subject, null, authorities));
                        } catch (Exception ignored) {
                            // Invalid token — authentication remains null, request will be rejected
                        }
                    }
                }
                chain.doFilter(request, response);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
