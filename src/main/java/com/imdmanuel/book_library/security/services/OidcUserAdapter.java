package com.imdmanuel.book_library.security.services;

import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

/**
 * Adapter class that wraps UserDetailsImpl to make it compatible with OIDC
 * while preserving access to the underlying UserDetailsImpl for JWT generation.
 */
public class OidcUserAdapter extends DefaultOidcUser {
    private final UserDetailsImpl userDetails;

    public OidcUserAdapter(UserDetailsImpl userDetails, OidcIdToken idToken, OidcUserInfo userInfo) {
        super(userDetails.getAuthorities(), idToken, userInfo);
        this.userDetails = userDetails;
    }

    /**
     * Get the underlying UserDetailsImpl for JWT generation and other operations
     */
    public UserDetailsImpl getUserDetails() {
        return userDetails;
    }
}

