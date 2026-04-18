package com.nazir.ecommerce.user.dto;

import com.nazir.ecommerce.common.enums.OAuthProvider;
import com.nazir.ecommerce.common.enums.UserRole;
import com.nazir.ecommerce.user.model.User;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {

    private final UUID id;
    private final String email;
    private final String fullName;
    private final String avatarUrl;
    private final String phoneNumber;
    private final UserRole role;
    private final OAuthProvider oauthProvider;
    private final boolean emailVerified;
    private final Instant createdAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .oauthProvider(user.getOauthProvider())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
