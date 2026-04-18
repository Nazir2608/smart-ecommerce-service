package com.nazir.ecommerce.auth.service;

import com.nazir.ecommerce.auth.dto.*;
import com.nazir.ecommerce.auth.model.RefreshToken;
import com.nazir.ecommerce.auth.repository.RefreshTokenRepository;
import com.nazir.ecommerce.common.enums.OAuthProvider;
import com.nazir.ecommerce.common.enums.UserRole;
import com.nazir.ecommerce.common.exception.AuthException;
import com.nazir.ecommerce.common.exception.BusinessException;
import com.nazir.ecommerce.infrastructure.email.EmailService;
import com.nazir.ecommerce.infrastructure.security.JwtService;
import com.nazir.ecommerce.infrastructure.security.UserPrincipal;
import com.nazir.ecommerce.user.model.User;
import com.nazir.ecommerce.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;
    private final EmailService emailService;

    @Value("${app.jwt.refresh-token-expiry-ms:604800000}") // 7 days
    private long refreshTokenExpiryMs;

    @Value("${app.jwt.access-token-expiry-ms:900000}")     // 15 min
    private long accessTokenExpiryMs;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "An account with this email already exists");
        }
        User user = User.builder()
                .email(request.getEmail().toLowerCase().strip())
                .fullName(request.getFullName().strip())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.BUYER)
                .oauthProvider(OAuthProvider.LOCAL)
                .emailVerified(true)   // simplified: auto-verify for now
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.info("New user registered: {}", saved.getEmail());

        emailService.sendWelcomeEmail(saved.getEmail(), saved.getFullName());
        return issueTokenPair(saved);
    }

    @Transactional
    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().strip(), request.getPassword()));

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = userRepository.findById(principal.getUserId()).orElseThrow(() -> new AuthException("User not found"));

        TokenResponse tokens = issueTokenPair(user, httpRequest.getHeader("User-Agent"), httpRequest.getRemoteAddr());

        log.info("User logged in: {}", user.getEmail());
        return tokens;
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (!stored.isValid()) {
            refreshTokenRepository.revokeAllByUserId(stored.getUser().getId());
            throw new AuthException("Refresh token is expired or revoked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        TokenResponse tokens = issueTokenPair(user, stored.getUserAgent(), stored.getIpAddress());
        log.debug("Tokens rotated for user {}", user.getEmail());
        return tokens;
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByToken(rawRefreshToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                    log.info("Refresh token revoked for user {}", token.getUser().getId());
                });
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().strip();

        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getOauthProvider() != OAuthProvider.LOCAL) {
                log.warn("Password reset attempted on OAuth account: {}", email);
                return;
            }
            String otp = otpService.generateAndStore(email);
            emailService.sendOtpEmail(email, otp);
            log.info("OTP dispatched for password reset: {}", email);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().toLowerCase().strip();

        otpService.verifyAndConsume(email, request.getOtp());

        User user = userRepository.findByEmail(email).orElseThrow(() -> new AuthException("No account found for this email"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate all existing sessions on password change
        int revoked = refreshTokenRepository.revokeAllByUserId(user.getId());
        log.info("Password reset for {}. {} sessions revoked.", email, revoked);
    }

    // Token pair factory (public for OAuth2 handler)
    public TokenResponse issueTokenPair(User user) {
        return issueTokenPair(user, null, null);
    }

    public TokenResponse issueTokenPair(User user, String userAgent, String ipAddress) {
        UserPrincipal principal = UserPrincipal.of(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = createAndPersistRefreshToken(user, userAgent, ipAddress);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(accessTokenExpiryMs / 1000)
                .build();
    }

    /**
     * Purge expired refresh tokens nightly at 02:00.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        int deleted = refreshTokenRepository.deleteExpiredTokens();
        log.info("Purged {} expired refresh tokens", deleted);
    }

    private String createAndPersistRefreshToken(User user, String userAgent, String ipAddress) {
        String tokenValue = UUID.randomUUID().toString();
        RefreshToken token = RefreshToken.builder()
                .token(tokenValue)
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpiryMs))
                .userAgent(userAgent)
                .ipAddress(ipAddress)
                .revoked(false)
                .build();
        refreshTokenRepository.save(token);
        return tokenValue;
    }
}
