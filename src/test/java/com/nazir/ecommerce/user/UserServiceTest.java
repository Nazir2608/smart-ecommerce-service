package com.nazir.ecommerce.user;

import com.nazir.ecommerce.common.enums.OAuthProvider;
import com.nazir.ecommerce.common.enums.UserRole;
import com.nazir.ecommerce.common.exception.BusinessException;
import com.nazir.ecommerce.infrastructure.security.UserPrincipal;
import com.nazir.ecommerce.user.dto.AddressRequest;
import com.nazir.ecommerce.user.dto.AddressResponse;
import com.nazir.ecommerce.user.dto.UpdateProfileRequest;
import com.nazir.ecommerce.user.dto.UserResponse;
import com.nazir.ecommerce.user.model.Address;
import com.nazir.ecommerce.user.model.User;
import com.nazir.ecommerce.user.repository.AddressRepository;
import com.nazir.ecommerce.user.repository.UserRepository;
import com.nazir.ecommerce.user.service.AddressService;
import com.nazir.ecommerce.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService & AddressService unit tests")
class UserServiceTest {

    @Mock UserRepository    userRepository;
    @Mock AddressRepository addressRepository;

    @InjectMocks UserService    userService;

    AddressService addressService;

    private User         testUser;
    private UserPrincipal testPrincipal;
    private UUID          userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("alice@example.com")
                .fullName("Alice Smith")
                .role(UserRole.BUYER)
                .oauthProvider(OAuthProvider.LOCAL)
                .emailVerified(true)
                .active(true)
                .build();
        testPrincipal = UserPrincipal.of(testUser);

        addressService = new AddressService(addressRepository, userService);
    }

    // ── UserService ───────────────────────────────────────────────

    @Nested
    @DisplayName("UserService.getProfile()")
    class GetProfile {

        @Test
        @DisplayName("returns UserResponse for authenticated user")
        void happyPath() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            UserResponse response = userService.getProfile(testPrincipal);

            assertThat(response.getEmail()).isEqualTo("alice@example.com");
            assertThat(response.getFullName()).isEqualTo("Alice Smith");
            assertThat(response.getRole()).isEqualTo(UserRole.BUYER);
        }
    }

    @Nested
    @DisplayName("UserService.updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("updates only provided fields")
        void partialUpdate() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setFullName("Alice Johnson");

            UserResponse response = userService.updateProfile(testPrincipal, req);

            assertThat(response.getFullName()).isEqualTo("Alice Johnson");
            assertThat(response.getEmail()).isEqualTo("alice@example.com"); // unchanged
        }

        @Test
        @DisplayName("clears avatar URL when empty string provided")
        void clearAvatarUrl() {
            testUser.setAvatarUrl("https://example.com/avatar.png");
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest req = new UpdateProfileRequest();
            req.setAvatarUrl("");

            UserResponse response = userService.updateProfile(testPrincipal, req);

            assertThat(response.getAvatarUrl()).isNull();
        }
    }

    // ── AddressService ────────────────────────────────────────────

    @Nested
    @DisplayName("AddressService.addAddress()")
    class AddAddress {

        @Test
        @DisplayName("first address is always set as default")
        void firstAddressBecomesDefault() {
            when(addressRepository.countByUserId(userId)).thenReturn(0);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

            AddressRequest req = buildValidAddressRequest(false);
            AddressResponse response = addressService.addAddress(testPrincipal, req);

            assertThat(response.isDefault()).isTrue();
        }

        @Test
        @DisplayName("throws when address limit exceeded")
        void exceedsLimit() {
            when(addressRepository.countByUserId(userId)).thenReturn(10);

            AddressRequest req = buildValidAddressRequest(false);

            assertThatThrownBy(() -> addressService.addAddress(testPrincipal, req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("at most 10");
        }

        @Test
        @DisplayName("clears previous default when new default is added")
        void clearsPreviousDefault() {
            when(addressRepository.countByUserId(userId)).thenReturn(1);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));

            AddressRequest req = buildValidAddressRequest(true); // isDefault = true

            addressService.addAddress(testPrincipal, req);

            verify(addressRepository).clearDefaultForUser(userId);
        }
    }

    @Nested
    @DisplayName("AddressService.listAddresses()")
    class ListAddresses {

        @Test
        @DisplayName("returns mapped responses ordered by default-first")
        void returnsAddresses() {
            Address defaultAddr = buildAddress(true);
            Address otherAddr   = buildAddress(false);
            when(addressRepository
                    .findByUserIdOrderByIsDefaultDescCreatedAtAsc(userId))
                    .thenReturn(List.of(defaultAddr, otherAddr));

            List<AddressResponse> list = addressService.listAddresses(testPrincipal);

            assertThat(list).hasSize(2);
            assertThat(list.get(0).isDefault()).isTrue();
        }
    }

    // ── Test helpers ──────────────────────────────────────────────

    private AddressRequest buildValidAddressRequest(boolean isDefault) {
        AddressRequest req = new AddressRequest();
        req.setFullName("Alice Smith");
        req.setPhoneNumber("+919876543210");
        req.setLine1("123 MG Road");
        req.setCity("Bengaluru");
        req.setState("Karnataka");
        req.setPostalCode("560001");
        req.setCountry("IN");
        req.setDefault(isDefault);
        return req;
    }

    private Address buildAddress(boolean isDefault) {
        return Address.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .fullName("Alice Smith")
                .phoneNumber("+919876543210")
                .line1("123 MG Road")
                .city("Bengaluru")
                .state("Karnataka")
                .postalCode("560001")
                .country("IN")
                .isDefault(isDefault)
                .build();
    }
}
