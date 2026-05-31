package com.bookingsystem.user.service;

import com.bookingsystem.user.domain.User;
import com.bookingsystem.user.dto.UserResponse;
import com.bookingsystem.user.exception.NotFoundException;
import com.bookingsystem.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserResponse findById(UUID id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    public List<UserResponse> listUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, Math.min(size, 100)))
                .map(this::toResponse)
                .toList();
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
