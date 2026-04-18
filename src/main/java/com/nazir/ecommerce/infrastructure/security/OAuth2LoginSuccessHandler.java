package com.nazir.ecommerce.infrastructure.security;

import com.nazir.ecommerce.auth.service.AuthService;
import com.nazir.ecommerce.common.enums.OAuthProvider;
import com.nazir.ecommerce.common.enums.UserRole;
import com.nazir.ecommerce.user.model.User;
import com.nazir.ecommerce.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final AuthService authService;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String oauthSubject = oAuth2User.getName(); // provider-unique subject ID

        if (email == null) {
            log.error("OAuth2 user has no email attribute from provider: {}", registrationId);
            getRedirectStrategy().sendRedirect(request, response, redirectUri + "?error=email_required");
            return;
        }

        OAuthProvider provider = "github".equalsIgnoreCase(registrationId)
                ? OAuthProvider.GITHUB : OAuthProvider.GOOGLE;

        // Find or create the user
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .fullName(name != null ? name : email.split("@")[0])
                    .avatarUrl(picture)
                    .role(UserRole.BUYER)
                    .oauthProvider(provider)
                    .oauthSubject(oauthSubject)
                    .emailVerified(true)   // OAuth email is already verified
                    .active(true)
                    .build();
            return userRepository.save(newUser);
        });

        // Update last-login fields for returning OAuth users
        if (user.getOauthProvider() == null) {
            user.setOauthProvider(provider);
            user.setOauthSubject(oauthSubject);
            userRepository.save(user);
        }

        // Issue tokens
        var tokenPair = authService.issueTokenPair(user);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken", tokenPair.getAccessToken())
                .queryParam("refreshToken", tokenPair.getRefreshToken())
                .build().toUriString();

        log.info("OAuth2 login success for {} via {}", email, registrationId);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
