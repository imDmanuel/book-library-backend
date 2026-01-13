package com.imdmanuel.book_library.security.services;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imdmanuel.book_library.models.Role;
import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.payload.request.ERole;
import com.imdmanuel.book_library.repository.RoleRepository;
import com.imdmanuel.book_library.repository.UserRepository;

@Service
public class CustomOidcUserService extends OidcUserService {
    private static final Logger logger = LoggerFactory.getLogger(CustomOidcUserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomOidcUserService(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest oidcUserRequest) throws OAuth2AuthenticationException {
        logger.info("Loading OIDC user from provider");
        OidcUser oidcUser = super.loadUser(oidcUserRequest);

        try {
            OidcUser result = processOidcUser(oidcUserRequest, oidcUser);
            logger.info("OIDC user processed successfully");
            return result;
        } catch (AuthenticationException ex) {
            logger.error("Authentication exception during OIDC processing: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            logger.error("Unexpected error processing OIDC user: ", ex);
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OidcUser processOidcUser(OidcUserRequest oidcUserRequest, OidcUser oidcUser) {
        String email = oidcUser.getEmail();
        logger.info("Processing OIDC user with email: {}", email);

        if (email == null || email.isEmpty()) {
            logger.error("Email not found in OIDC attributes");
            throw new OAuth2AuthenticationException("Email not found from OIDC provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            logger.info("Existing user found with email: {}", email);
            user = userOptional.get();
        } else {
            logger.info("Creating new user for email: {}", email);
            user = registerNewOidcUser(oidcUserRequest, oidcUser);
        }

        // Return UserDetailsImpl wrapped in OidcUserAdapter to maintain OIDC compatibility
        UserDetailsImpl userDetails = UserDetailsImpl.build(user, oidcUser.getAttributes());
        logger.info("Built UserDetailsImpl for user ID: {}, Email: {}", userDetails.getId(), userDetails.getEmail());
        
        return new OidcUserAdapter(userDetails, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private User registerNewOidcUser(OidcUserRequest oidcUserRequest, OidcUser oidcUser) {
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        // Defensive check - email should never be null here, but add check for safety
        if (email == null || email.isEmpty()) {
            logger.error("Email is null or empty in registerNewOidcUser");
            throw new OAuth2AuthenticationException("Email is required for user registration");
        }

        User user = new User();
        user.setUsername(name != null ? name : email.split("@")[0]); // Use email prefix if name is null
        user.setEmail(email);

        // Set a dummy password for OIDC users as they don't have one
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        // Assign default role for new OIDC users
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER.name())
                .orElseThrow(() -> {
                    logger.error("ROLE_USER not found in database");
                    return new RuntimeException("Error: Role is not found");
                });
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        logger.info("New OIDC user registered with ID: {}", savedUser.getId());
        return savedUser;
    }
}

