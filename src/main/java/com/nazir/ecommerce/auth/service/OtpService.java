package com.nazir.ecommerce.auth.service;

import com.nazir.ecommerce.common.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final String OTP_KEY_PREFIX   = "otp:pwd:";
    private static final String RATE_KEY_PREFIX  = "otp:rate:";
    private static final int    MAX_ATTEMPTS_PER_HOUR = 5;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom        secureRandom = new SecureRandom();

    @Value("${app.otp.expiry-minutes:10}")
    private long otpExpiryMinutes;

    // ── Generate & store ───────────────────────────────────────────

    /**
     * Generates a 6-digit OTP, stores it in Redis keyed by email,
     * and returns the plain OTP for the caller to email.
     * Enforces a rate limit of {@value MAX_ATTEMPTS_PER_HOUR} requests per hour.
     */
    public String generateAndStore(String email) {
        enforceRateLimit(email);

        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        String key = OTP_KEY_PREFIX + email.toLowerCase();
        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(otpExpiryMinutes));
        log.debug("OTP stored for {} (expires in {} min)", email, otpExpiryMinutes);
        return otp;
    }

    // ── Verify ─────────────────────────────────────────────────────

    /**
     * Validates the OTP. Deletes it from Redis on success (one-time use).
     * Throws {@link AuthException} if the OTP is invalid or expired.
     */
    public void verifyAndConsume(String email, String submittedOtp) {
        String key  = OTP_KEY_PREFIX + email.toLowerCase();
        String stored = redisTemplate.opsForValue().get(key);

        if (stored == null) {
            throw new AuthException("OTP has expired or was never issued. Please request a new one.");
        }
        if (!stored.equals(submittedOtp)) {
            throw new AuthException("Invalid OTP. Please check your email and try again.");
        }

        // Consume: delete so it cannot be reused
        redisTemplate.delete(key);
        log.info("OTP verified and consumed for {}", email);
    }

    // ── Rate limiting ──────────────────────────────────────────────

    private void enforceRateLimit(String email) {
        String rateKey = RATE_KEY_PREFIX + email.toLowerCase();
        Long   count   = redisTemplate.opsForValue().increment(rateKey);

        if (count != null && count == 1) {
            // First request in this window — set TTL
            redisTemplate.expire(rateKey, Duration.ofHours(1));
        }

        if (count != null && count > MAX_ATTEMPTS_PER_HOUR) {
            throw new AuthException(
                "Too many OTP requests. Please wait before requesting another password reset.");
        }
    }
}
