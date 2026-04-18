package com.nazir.ecommerce.user.service;

import com.nazir.ecommerce.common.exception.ResourceNotFoundException;
import com.nazir.ecommerce.infrastructure.security.UserPrincipal;
import com.nazir.ecommerce.user.dto.UpdateProfileRequest;
import com.nazir.ecommerce.user.dto.UserResponse;
import com.nazir.ecommerce.user.model.User;
import com.nazir.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    // ── Read ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserResponse getProfile(UserPrincipal principal) {
        User user = findUserById(principal.getUserId());
        return UserResponse.from(user);
    }

    @Transactional(readOnly = true)
    public User findById(UUID userId) {
        return findUserById(userId);
    }

    // ── Update ─────────────────────────────────────────────────────

    @Transactional
    public UserResponse updateProfile(UserPrincipal principal, UpdateProfileRequest request) {
        User user = findUserById(principal.getUserId());

        if (StringUtils.hasText(request.getFullName())) {
            user.setFullName(request.getFullName());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(StringUtils.hasText(request.getAvatarUrl())
                    ? request.getAvatarUrl() : null);
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(StringUtils.hasText(request.getPhoneNumber())
                    ? request.getPhoneNumber() : null);
        }

        User saved = userRepository.save(user);
        log.info("Profile updated for user {}", saved.getId());
        return UserResponse.from(saved);
    }

    // ── Internal helpers ───────────────────────────────────────────

    public User findUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
