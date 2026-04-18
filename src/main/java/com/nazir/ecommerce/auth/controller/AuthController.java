package com.nazir.ecommerce.auth.controller;

import com.nazir.ecommerce.auth.dto.*;
import com.nazir.ecommerce.auth.service.AuthService;
import com.nazir.ecommerce.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh, OAuth2 and password reset")
public class AuthController {

    private final AuthService authService;

    // ── POST /register ─────────────────────────────────────────────

    @PostMapping("/register")
    @Operation(
        summary  = "Register a new user account",
        description = "Creates a BUYER account. Returns access + refresh tokens on success.")
    public ResponseEntity<ApiResponse<TokenResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        TokenResponse tokens = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(tokens, "Account created successfully"));
    }

    // ── POST /login ────────────────────────────────────────────────

    @PostMapping("/login")
    @Operation(
        summary     = "Login with email and password",
        description = "Returns a short-lived access token (15 min) and a long-lived refresh token (7 days).")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        TokenResponse tokens = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(tokens, "Login successful"));
    }

    // ── POST /refresh ──────────────────────────────────────────────

    @PostMapping("/refresh")
    @Operation(
        summary     = "Refresh the access token",
        description = "Accepts a valid refresh token and issues a new rotated token pair.")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        TokenResponse tokens = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.ok(tokens, "Token refreshed"));
    }

    // ── POST /logout ───────────────────────────────────────────────

    @PostMapping("/logout")
    @Operation(
        summary     = "Logout — revoke refresh token",
        description = "Marks the provided refresh token as revoked. " +
                      "The access token will expire naturally.")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request) {

        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    // ── POST /forgot-password ──────────────────────────────────────

    @PostMapping("/forgot-password")
    @Operation(
        summary     = "Request a password reset OTP",
        description = "Sends a 6-digit OTP to the registered email. " +
                      "Always returns 200 to prevent user enumeration.")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "If an account exists for this email, an OTP has been sent"));
    }

    // ── POST /reset-password ───────────────────────────────────────

    @PostMapping("/reset-password")
    @Operation(
        summary     = "Reset password using OTP",
        description = "Verifies the OTP, updates the password, and revokes all existing sessions.")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Password reset successfully. Please log in with your new password."));
    }

    // ── GET /oauth2/google — handled by Spring OAuth2 auto-config ──
    // Spring Security's built-in OAuth2 redirect endpoint is:
    //   GET /oauth2/authorization/google
    // The alias below just explains this in the Swagger UI.

    @GetMapping("/oauth2/google")
    @Operation(
        summary     = "Initiate Google OAuth2 login",
        description = "Redirects the browser to Google's consent screen. " +
                      "On success, Google redirects back and the server issues tokens " +
                      "then forwards them to the configured frontend redirect URI.")
    public ResponseEntity<ApiResponse<Void>> googleOAuth2Info() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Redirect your browser to: /oauth2/authorization/google"));
    }
}
