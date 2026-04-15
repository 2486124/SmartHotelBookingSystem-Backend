package com.cts.project.shbs.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.cts.project.shbs.client.LoyaltyServiceClient;
import com.cts.project.shbs.dto.JwtResponse;
import com.cts.project.shbs.dto.LoginRequest;
import com.cts.project.shbs.dto.RegisterRequest;
import com.cts.project.shbs.exception.AccountNotFoundException;
import com.cts.project.shbs.exception.EmailAlreadyExistsException;
import com.cts.project.shbs.exception.InvalidOrExpiredTokenException;
import com.cts.project.shbs.exception.PasswordResetTokenAlreadyUsedException;
import com.cts.project.shbs.exception.UserNotFoundException;
import com.cts.project.shbs.model.Role;
import com.cts.project.shbs.model.User;
import com.cts.project.shbs.repository.UserRepository;
import com.cts.project.shbs.security.JwtUtils;
import com.cts.project.shbs.service.EmailService;
import com.cts.project.shbs.service.UserService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final EmailService emailService;
    private final LoyaltyServiceClient loyaltyServiceClient;
    private final AuthenticationManager authenticationManager;
    
    private static final String LOYALTY_CB = "loyaltyService";
    
    @Lazy
    @Autowired
    private UserServiceImpl self;

    @Override
    public User registerUser(RegisterRequest request) {
        log.info("Attempting to register user with email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already in use: {}", request.getEmail());
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setContactNumber(request.getContactNumber());

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {} and role: {}", savedUser.getUserId(), savedUser.getRole());

        // Initialize loyalty account for GUEST users only
        if (Role.ROLE_GUEST.equals(savedUser.getRole())) {
            try {
                self.initializeLoyaltyAccount(savedUser.getUserId());
            } catch (Exception e) {
                log.warn("Loyalty initialization failed for user ID: {}. Will retry later. Reason: {}",
                        savedUser.getUserId(), e.getMessage());
            }
        }

        return savedUser;
    }
    
    @CircuitBreaker(name = LOYALTY_CB, fallbackMethod = "loyaltyFallback")
    @Retry(name = LOYALTY_CB, fallbackMethod = "loyaltyFallback")
    public void initializeLoyaltyAccount(long userId) {
        log.info("Initializing loyalty account for GUEST user ID: {}", userId);
        loyaltyServiceClient.initializeLoyaltyAccount((long) userId);
        log.info("Loyalty account initialized successfully for user ID: {}", userId);
    }

    /**
     * Fallback when circuit is OPEN or all retries exhausted.
     */
    public void loyaltyFallback(long userId, Throwable ex) {
        log.warn("Loyalty service unavailable for user ID: {}. Reason: {}. " +
                 "Account will be initialized on next retry job.", userId, ex.getMessage());
        
    }
    
    @Override
    public JwtResponse loginUser(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        org.springframework.security.core.userdetails.User userDetails =
                (org.springframework.security.core.userdetails.User) authentication.getPrincipal();

        Long userId = Long.parseLong(userDetails.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Login failed - user not found with ID: {}", userId);
                    return new UserNotFoundException(userId);
                });

        log.info("Login successful for user ID: {}, role: {}", user.getUserId(), user.getRole());
        return new JwtResponse(jwt, user.getUserId(), user.getName(), user.getEmail(), user.getRole().name());
    }

    @Override
    public User getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new UserNotFoundException(id);
                });
    }

    @Override
    public List<User> getAllUsers() {
        log.debug("Fetching all users");
        List<User> users = userRepository.findAll();
        log.info("Total users fetched: {}", users.size());
        return users;
    }

    @Override
    public void deleteUser(Long id) {
        log.info("Attempting to delete user with ID: {}", id);
        if (!userRepository.existsById(id)) {
            log.warn("Delete failed - user not found with ID: {}", id);
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
        log.info("User deleted successfully with ID: {}", id);
    }

    @Override
    public User updateUserProfile(Long id, RegisterRequest request) {
        log.info("Updating profile for user ID: {}", id);
        User existingUser = getUserById(id);

        existingUser.setName(request.getName());
        existingUser.setContactNumber(request.getContactNumber());

        User updatedUser = userRepository.save(existingUser);
        log.info("Profile updated successfully for user ID: {}", id);
        return updatedUser;
    }

    @Override
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            log.debug("Search keyword is empty, returning all users");
            return userRepository.findAll();
        }
        log.debug("Searching users with keyword: {}", keyword.trim());
        List<User> results = userRepository.searchByKeyword(keyword.trim());
        log.info("Search for '{}' returned {} result(s)", keyword.trim(), results.size());
        return results;
    }

    @Override
    public void forgotPassword(String email) {
        log.info("Password reset requested for email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Forgot password failed - no account found for email: {}", email);
                    return new AccountNotFoundException(email);
                });

        String resetToken = jwtUtils.generatePasswordResetToken(user);
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        log.info("Password reset email sent successfully to: {}", email);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        log.info("Attempting to reset password using token");

        Claims claims;
        try {
            claims = jwtUtils.validatePasswordResetToken(token);
        } catch (JwtException e) {
            log.warn("Password reset failed - invalid or expired token: {}", e.getMessage());
            throw new InvalidOrExpiredTokenException();
        }

        Long userId = Long.parseLong(claims.getSubject());
        log.debug("Reset token validated for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Reset password failed - user not found with ID: {}", userId);
                    return new UserNotFoundException(userId);
                });

        // Single-use check — if password already changed, token is dead
        String oldPasswordHash = claims.get("oldPasswordHash", String.class);
        if (!user.getPassword().equals(oldPasswordHash)) {
            log.warn("Password reset failed - token already used for user ID: {}", userId);
            throw new PasswordResetTokenAlreadyUsedException();
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password reset successfully for user ID: {}", userId);
    }
}