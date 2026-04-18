package com.nazir.ecommerce.user.service;

import com.nazir.ecommerce.common.exception.BusinessException;
import com.nazir.ecommerce.common.exception.ResourceNotFoundException;
import com.nazir.ecommerce.infrastructure.security.UserPrincipal;
import com.nazir.ecommerce.user.dto.AddressRequest;
import com.nazir.ecommerce.user.dto.AddressResponse;
import com.nazir.ecommerce.user.model.Address;
import com.nazir.ecommerce.user.model.User;
import com.nazir.ecommerce.user.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private static final int MAX_ADDRESSES_PER_USER = 10;

    private final AddressRepository addressRepository;
    private final UserService userService;

    // ── Read ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(UserPrincipal principal) {
        return addressRepository
                .findByUserIdOrderByIsDefaultDescCreatedAtAsc(principal.getUserId())
                .stream()
                .map(AddressResponse::from)
                .toList();
    }

    // ── Create ─────────────────────────────────────────────────────

    @Transactional
    public AddressResponse addAddress(UserPrincipal principal, AddressRequest request) {
        UUID userId = principal.getUserId();

        int count = addressRepository.countByUserId(userId);
        if (count >= MAX_ADDRESSES_PER_USER) {
            throw new BusinessException("ADDRESS_LIMIT_EXCEEDED",
                    "You can have at most " + MAX_ADDRESSES_PER_USER + " saved addresses");
        }

        // If this is marked as default, clear existing default first
        if (request.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        }

        // First address is always default
        boolean shouldBeDefault = request.isDefault() || count == 0;

        User user = userService.findUserById(userId);
        Address address = Address.builder()
                .user(user)
                .label(request.getLabel())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .line1(request.getLine1())
                .line2(request.getLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry() != null ? request.getCountry() : "IN")
                .isDefault(shouldBeDefault)
                .build();

        Address saved = addressRepository.save(address);
        log.info("Address {} created for user {}", saved.getId(), userId);
        return AddressResponse.from(saved);
    }

    // ── Update ─────────────────────────────────────────────────────

    @Transactional
    public AddressResponse updateAddress(UserPrincipal principal, UUID addressId,
                                         AddressRequest request) {
        UUID userId = principal.getUserId();
        Address address = findAddressByIdAndUser(addressId, userId);

        if (request.isDefault() && !address.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        }

        address.setLabel(request.getLabel());
        address.setFullName(request.getFullName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setLine1(request.getLine1());
        address.setLine2(request.getLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry() != null ? request.getCountry() : "IN");
        address.setDefault(request.isDefault());

        Address saved = addressRepository.save(address);
        log.info("Address {} updated for user {}", saved.getId(), userId);
        return AddressResponse.from(saved);
    }

    // ── Delete ─────────────────────────────────────────────────────

    @Transactional
    public void deleteAddress(UserPrincipal principal, UUID addressId) {
        UUID userId = principal.getUserId();
        Address address = findAddressByIdAndUser(addressId, userId);

        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);
        log.info("Address {} deleted for user {}", addressId, userId);

        // Promote the oldest remaining address to default if the deleted one was default
        if (wasDefault) {
            addressRepository
                    .findByUserIdOrderByIsDefaultDescCreatedAtAsc(userId)
                    .stream()
                    .findFirst()
                    .ifPresent(a -> {
                        a.setDefault(true);
                        addressRepository.save(a);
                    });
        }
    }

    // ── Helpers ────────────────────────────────────────────────────

    private Address findAddressByIdAndUser(UUID addressId, UUID userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
    }
}
