package com.imdmanuel.book_library.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.imdmanuel.book_library.security.jwt.AuthEntryPointJwt;
import com.imdmanuel.book_library.security.jwt.AuthTokenFilter;
import com.imdmanuel.book_library.security.services.CustomOauth2UserService;
import com.imdmanuel.book_library.security.services.CustomOidcUserService;
import com.imdmanuel.book_library.security.services.OidcUserAdapter;
import com.imdmanuel.book_library.security.services.UserDetailsImpl;
import com.imdmanuel.book_library.security.services.UserDetailsServiceImpl;
import com.imdmanuel.book_library.security.jwt.JwtUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {
    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final CustomOauth2UserService customOauth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final AuthTokenFilter authTokenFilter;
    private final RequestLoggingFilter requestLoggingFilter;
    private final JwtUtils jwtUtils;
    @NonNull
    private final String frontendRedirectUrl;

    public WebSecurityConfig(UserDetailsServiceImpl userDetailsService, AuthEntryPointJwt unauthorizedHandler,
            AuthTokenFilter authTokenFilter, @Lazy CustomOauth2UserService customOauth2UserService,
            @Lazy CustomOidcUserService customOidcUserService, RequestLoggingFilter requestLoggingFilter,
            JwtUtils jwtUtils, @Value("${frontend.redirect.url}") @NonNull String frontendRedirectUrl) {
        this.userDetailsService = userDetailsService;
        this.unauthorizedHandler = unauthorizedHandler;
        this.authTokenFilter = authTokenFilter;
        this.customOauth2UserService = customOauth2UserService;
        this.customOidcUserService = customOidcUserService;
        this.requestLoggingFilter = requestLoggingFilter;
        this.jwtUtils = jwtUtils;
        this.frontendRedirectUrl = frontendRedirectUrl;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        // authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/api/auth/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-ui/index.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/swagger-resources/**",
                                "/swagger-resources",
                                "/webjars/**",
                                "/configuration/**",
                                "/configuration/security",
                                "/configuration/ui")
                        .permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/login/oauth2/code/**").permitAll()
                        .anyRequest()
                        .authenticated())
                .oauth2Login(
                        // Use our custom services for both OAuth2 and OIDC providers
                        oauth2 -> oauth2.userInfoEndpoint(userInfo -> userInfo
                                .userService(customOauth2UserService)
                                .oidcUserService(customOidcUserService))
                                .successHandler(oAuth2AuthenticationSuccessHandler())) // Handle successful Oauth2 login
        ;

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(requestLoggingFilter, AuthTokenFilter.class);

        return http.build();
    }

    // OAuth2 Success handler, handles what happens after a successful OAuth2/OIDC
    // login.
    // Both CustomOauth2UserService and CustomOidcUserService return
    // UserDetailsImpl,
    // so we just need to generate JWT and redirect.
    @Bean
    public AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);
            logger.info("OAuth2 authentication success handler called");
            logger.info("Principal type: {}",
                    authentication != null ? authentication.getPrincipal().getClass().getName() : "null");

            if (authentication == null) {
                logger.error("Authentication is null");
                redirectToError(response, "oauth_2_login_failed");
                return;
            }

            UserDetailsImpl userDetails = null;

            // Extract UserDetailsImpl from different principal types
            if (authentication.getPrincipal() instanceof OidcUserAdapter) {
                // OIDC user (from CustomOidcUserService)
                OidcUserAdapter oidcUserAdapter = (OidcUserAdapter) authentication.getPrincipal();
                userDetails = oidcUserAdapter.getUserDetails();
                logger.info("OidcUserAdapter found - ID: {}, Email: {}", userDetails.getId(), userDetails.getEmail());
            } else if (authentication.getPrincipal() instanceof UserDetailsImpl) {
                // Direct UserDetailsImpl (from CustomOauth2UserService for non-OIDC)
                userDetails = (UserDetailsImpl) authentication.getPrincipal();
                logger.info("UserDetailsImpl found - ID: {}, Email: {}", userDetails.getId(), userDetails.getEmail());
            } else {
                logger.error("Unsupported principal type: {}. Expected UserDetailsImpl or OidcUserAdapter.",
                        authentication.getPrincipal().getClass().getName());
                redirectToError(response, "oauth_2_login_failed");
                return;
            }

            // Create a new Authentication with UserDetailsImpl as principal for JWT
            // generation
            // (needed when principal is OidcUserAdapter)
            Authentication jwtAuthentication = authentication;
            if (authentication.getPrincipal() instanceof OidcUserAdapter) {
                jwtAuthentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
            }

            // Generate JWT token and redirect
            String jwt = jwtUtils.generateJwtToken(jwtAuthentication);
            logger.info("JWT token generated successfully");
            redirectWithToken(response, jwt, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail());
        };
    }

    private void redirectWithToken(HttpServletResponse response, String jwt, Long id, String username, String email)
            throws IOException {
        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontendRedirectUrl)
                .queryParam("token", jwt)
                .queryParam("id", id)
                .queryParam("username", username)
                .queryParam("email", email)
                .build()
                .toUriString();

        LoggerFactory.getLogger(WebSecurityConfig.class).info("Redirecting to: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }

    private void redirectToError(HttpServletResponse response, String error) throws IOException {
        String errorUrl = UriComponentsBuilder
                .fromUriString(frontendRedirectUrl)
                .queryParam("error", error)
                .build()
                .toUriString();

        LoggerFactory.getLogger(WebSecurityConfig.class).info("Redirecting to error URL: {}", errorUrl);
        response.sendRedirect(errorUrl);
    }

}
