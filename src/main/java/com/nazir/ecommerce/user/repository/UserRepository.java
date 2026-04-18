package com.nazir.ecommerce.user.repository;

import com.nazir.ecommerce.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByOauthSubjectAndOauthProvider(
            String oauthSubject,
            com.nazir.ecommerce.common.enums.OAuthProvider provider);
}
