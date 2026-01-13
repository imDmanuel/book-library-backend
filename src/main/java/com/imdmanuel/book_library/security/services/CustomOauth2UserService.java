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
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.imdmanuel.book_library.models.Role;
import com.imdmanuel.book_library.models.User;
import com.imdmanuel.book_library.payload.request.ERole;
import com.imdmanuel.book_library.repository.RoleRepository;
import com.imdmanuel.book_library.repository.UserRepository;

@Service
public class CustomOauth2UserService extends DefaultOAuth2UserService {
    private static final Logger logger = LoggerFactory.getLogger(CustomOauth2UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomOauth2UserService(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        logger.info("Loading OAuth2 user from provider");
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            OAuth2User result = processOAuth2User(oAuth2UserRequest, oAuth2User);
            logger.info("OAuth2 user processed successfully");
            return result;
        } catch (AuthenticationException ex) {
            logger.error("Authentication exception during OAuth2 processing: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            logger.error("Unexpected error processing OAuth2 user: ", ex);
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        logger.info("Processing OAuth2 user with email: {}", email);

        if (email == null || email.isEmpty()) {
            logger.error("Email not found in OAuth2 attributes");
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            logger.info("Existing user found with email: {}", email);
            user = userOptional.get();
        } else {
            logger.info("Creating new user for email: {}", email);
            user = registerNewOauth2User(oAuth2UserRequest, oAuth2User);
        }

        // Return a custom UserDetailsImpl object that includes the OAuth2User
        // attributes, this is crucial for jwt generation later
        UserDetailsImpl userDetails = UserDetailsImpl.build(user, oAuth2User.getAttributes());
        logger.info("Built UserDetailsImpl for user ID: {}, Email: {}", userDetails.getId(), userDetails.getEmail());
        return userDetails;
    }

    private User registerNewOauth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        // Defensive check - email should never be null here, but add check for safety
        if (email == null || email.isEmpty()) {
            logger.error("Email is null or empty in registerNewOauth2User");
            throw new OAuth2AuthenticationException("Email is required for user registration");
        }

        User user = new User();
        user.setUsername(name != null ? name : email.split("@")[0]); // Use email prefix if name is null
        user.setEmail(email);

        // Set a dummy password for OAuth2 users as they don't have one
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        // Assign default role for new OAuth2 users
        Set<Role> roles = new HashSet<>();
        Role userRole = roleRepository.findByName(ERole.ROLE_USER.name())
                .orElseThrow(() -> {
                    logger.error("ROLE_USER not found in database");
                    return new RuntimeException("Error: Role is not found");
                });
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        logger.info("New OAuth2 user registered with ID: {}", savedUser.getId());
        return savedUser;
    }
}
