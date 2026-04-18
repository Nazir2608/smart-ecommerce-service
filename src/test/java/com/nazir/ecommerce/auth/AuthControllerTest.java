package com.nazir.ecommerce.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nazir.ecommerce.auth.dto.*;
import com.nazir.ecommerce.auth.service.AuthService;
import com.nazir.ecommerce.common.exception.AuthException;
import com.nazir.ecommerce.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController integration tests")
class AuthControllerTest {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper mapper;

    @MockBean AuthService authService;

    private static final TokenResponse MOCK_TOKENS = TokenResponse.builder()
            .accessToken("mock.access.token")
            .refreshToken("mock-refresh-uuid")
            .expiresIn(900)
            .build();

    // ── Register ─────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class Register {

        @Test
        @DisplayName("returns 201 with tokens on valid request")
        void happyPath() throws Exception {
            when(authService.register(any())).thenReturn(MOCK_TOKENS);

            RegisterRequest body = new RegisterRequest();
            body.setFullName("Alice Smith");
            body.setEmail("alice@example.com");
            body.setPassword("Password1");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("mock.access.token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("mock-refresh-uuid"));
        }

        @Test
        @DisplayName("returns 400 when email already exists")
        void duplicateEmail() throws Exception {
            when(authService.register(any()))
                    .thenThrow(new BusinessException("EMAIL_ALREADY_EXISTS",
                            "An account with this email already exists"));

            RegisterRequest body = new RegisterRequest();
            body.setFullName("Bob");
            body.setEmail("bob@example.com");
            body.setPassword("Password1");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
        }

        @Test
        @DisplayName("returns 400 on invalid payload — weak password")
        void invalidPassword() throws Exception {
            RegisterRequest body = new RegisterRequest();
            body.setFullName("Charlie");
            body.setEmail("charlie@example.com");
            body.setPassword("weak"); // fails @Pattern

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    // ── Login ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("returns 200 with tokens on correct credentials")
        void happyPath() throws Exception {
            when(authService.login(any(), any())).thenReturn(MOCK_TOKENS);

            LoginRequest body = new LoginRequest();
            body.setEmail("alice@example.com");
            body.setPassword("Password1");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("returns 401 on bad credentials")
        void badCredentials() throws Exception {
            when(authService.login(any(), any()))
                    .thenThrow(new org.springframework.security.authentication
                            .BadCredentialsException("Bad credentials"));

            LoginRequest body = new LoginRequest();
            body.setEmail("alice@example.com");
            body.setPassword("wrongpassword");

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── Refresh ──────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class Refresh {

        @Test
        @DisplayName("returns new token pair for valid refresh token")
        void happyPath() throws Exception {
            when(authService.refresh(any())).thenReturn(MOCK_TOKENS);

            RefreshTokenRequest body = new RefreshTokenRequest();
            body.setRefreshToken("valid-refresh-uuid");

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").exists());
        }

        @Test
        @DisplayName("returns 401 for expired refresh token")
        void expiredToken() throws Exception {
            when(authService.refresh(any()))
                    .thenThrow(new AuthException("Refresh token is expired or revoked"));

            RefreshTokenRequest body = new RefreshTokenRequest();
            body.setRefreshToken("expired-token");

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── Logout ───────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("returns 200 and revokes token")
        void happyPath() throws Exception {
            doNothing().when(authService).logout(any());

            RefreshTokenRequest body = new RefreshTokenRequest();
            body.setRefreshToken("some-refresh-uuid");

            mockMvc.perform(post("/api/v1/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));
        }
    }

    // ── Forgot password ──────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password")
    class ForgotPassword {

        @Test
        @DisplayName("always returns 200 (prevents user enumeration)")
        void alwaysOk() throws Exception {
            doNothing().when(authService).forgotPassword(any());

            ForgotPasswordRequest body = new ForgotPasswordRequest();
            body.setEmail("unknown@example.com");

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ── Reset password ───────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPassword {

        @Test
        @DisplayName("returns 200 on valid OTP + new password")
        void happyPath() throws Exception {
            doNothing().when(authService).resetPassword(any());

            ResetPasswordRequest body = new ResetPasswordRequest();
            body.setEmail("alice@example.com");
            body.setOtp("123456");
            body.setNewPassword("NewPass1");

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 401 on wrong OTP")
        void wrongOtp() throws Exception {
            doThrow(new AuthException("Invalid OTP"))
                    .when(authService).resetPassword(any());

            ResetPasswordRequest body = new ResetPasswordRequest();
            body.setEmail("alice@example.com");
            body.setOtp("000000");
            body.setNewPassword("NewPass1");

            mockMvc.perform(post("/api/v1/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
