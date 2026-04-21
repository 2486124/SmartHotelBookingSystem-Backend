package com.cts.project.shbs.controller;

import com.cts.project.shbs.dto.ForgotPasswordRequest;
import com.cts.project.shbs.dto.JwtResponse;
import com.cts.project.shbs.dto.LoginRequest;
import com.cts.project.shbs.dto.RegisterRequest;
import com.cts.project.shbs.dto.ResetPasswordRequest;
import com.cts.project.shbs.exception.AccountNotFoundException;
import com.cts.project.shbs.model.User;
import com.cts.project.shbs.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Auth Controller", description = "Handles user registration, authentication, profile management and admin operations")
public class AuthController {

    private final UserService userService;

    // --------------------------------------------------------
    // AUTH
    // --------------------------------------------------------

    @Operation(summary = "Register a new user", description = "Creates a new user account with the provided details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or email already exists", content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        log.info("POST /register - Request received for email: {}", signUpRequest.getEmail());
        User registeredUser = userService.registerUser(signUpRequest);
        log.info("POST /register - User registered successfully with ID: {}", registeredUser.getUserId());
        return ResponseEntity.ok("User registered successfully!");
    }

    @Operation(summary = "Login user", description = "Authenticates user credentials and returns a JWT token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful, JWT returned",
            content = @Content(schema = @Schema(implementation = JwtResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid email or password", content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("POST /login - Login attempt for email: {}", loginRequest.getEmail());
        JwtResponse response = userService.loginUser(loginRequest);
        log.info("POST /login - Login successful for user ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    // --------------------------------------------------------
    // FORGOT / RESET PASSWORD
    // --------------------------------------------------------

    @Operation(summary = "Forgot password", description = "Sends a password reset link to the registered email if it exists")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reset link sent if email is registered"),
        @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("POST /forgot-password - Request received for email: {}", request.getEmail());
        try {
            userService.forgotPassword(request.getEmail());
        } catch (AccountNotFoundException e) {
            log.debug("POST /forgot-password - Email not found, suppressing response: {}", request.getEmail());
        } catch (Exception e) {
            log.error("POST /forgot-password - Unexpected error for email {}: {}", request.getEmail(), e.getMessage(), e);
        }
        return ResponseEntity.ok("If that email is registered, a reset link has been sent.");
    }

    @Operation(summary = "Reset password", description = "Resets the user password using a valid reset token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid or expired token", content = @Content)
    })
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("POST /reset-password - Password reset attempt received");
        userService.resetPassword(request.getToken(), request.getNewPassword());
        log.info("POST /reset-password - Password reset completed successfully");
        return ResponseEntity.ok("Password reset successfully. Please login with your new password.");
    }

    // --------------------------------------------------------
    // PROFILE MANAGEMENT
    // --------------------------------------------------------

    @Operation(
        summary = "Get own profile",
        description = "Returns the profile of the currently authenticated user. JWT is validated at the API gateway — the resolved user ID is forwarded via the X-User-Id header.",
        parameters = {
            @Parameter(
                name = "X-User-Id",
                in = ParameterIn.HEADER,
                required = true,
                description = "Authenticated user's ID, injected by the API gateway after JWT validation",
                schema = @Schema(type = "string", example = "42")
            )
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile fetched successfully",
            content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized — JWT rejected at gateway", content = @Content)
    })
    @GetMapping("/profile")
    public ResponseEntity<?> getOwnProfile(
            @RequestHeader("X-User-Id") String userIdHeader) {
        Long userId = Long.parseLong(userIdHeader);
        log.info("GET /profile - Request received for user ID: {}", userId);
        User user = userService.getUserById(userId);
        log.info("GET /profile - Profile fetched successfully for user ID: {}", userId);
        return ResponseEntity.ok(user);
    }

    @Operation(
        summary = "Update own profile",
        description = "Updates the profile of the currently authenticated user. JWT is validated at the API gateway — the resolved user ID is forwarded via the X-User-Id header.",
        parameters = {
            @Parameter(
                name = "X-User-Id",
                in = ParameterIn.HEADER,
                required = true,
                description = "Authenticated user's ID, injected by the API gateway after JWT validation",
                schema = @Schema(type = "string", example = "42")
            )
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile updated successfully",
            content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized — JWT rejected at gateway", content = @Content)
    })
    @PutMapping("/profile")
    public ResponseEntity<?> updateOwnProfile(
            @RequestHeader("X-User-Id") String userIdHeader,
            @Valid @RequestBody RegisterRequest updateRequest) {
        Long userId = Long.parseLong(userIdHeader);
        log.info("PUT /profile - Update request received for user ID: {}", userId);
        User updatedUser = userService.updateUserProfile(userId, updateRequest);
        log.info("PUT /profile - Profile updated successfully for user ID: {}", userId);
        return ResponseEntity.ok(updatedUser);
    }

    // --------------------------------------------------------
    // ADMIN ONLY ROUTES
    // --------------------------------------------------------

    @Operation(
        summary = "Get all users",
        description = "Admin only — returns a list of all registered users. JWT is validated at the API gateway — the resolved role is forwarded via the X-User-Role header.",
        parameters = {
            @Parameter(
                name = "X-User-Role",
                in = ParameterIn.HEADER,
                required = true,
                description = "Authenticated user's role, injected by the API gateway. Must be ROLE_ADMIN.",
                schema = @Schema(type = "string", example = "ROLE_ADMIN")
            )
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Users fetched successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden — Admin role required", content = @Content)
    })
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader("X-User-Role") String userRole) {
        if (!userRole.equals("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Admin role required.");
        }
        log.info("GET /users - Admin request to fetch all users");
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @Operation(
        summary = "Get user by ID",
        description = "Admin only — returns a specific user by their ID. JWT is validated at the API gateway — the resolved role is forwarded via the X-User-Role header.",
        parameters = {
            @Parameter(
                name = "X-User-Role",
                in = ParameterIn.HEADER,
                required = true,
                description = "Authenticated user's role, injected by the API gateway. Must be ROLE_ADMIN.",
                schema = @Schema(type = "string", example = "ROLE_ADMIN")
            )
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found",
            content = @Content(schema = @Schema(implementation = User.class))),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden — Admin role required", content = @Content)
    })
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(
            @RequestHeader("X-User-Role") String userRole,
            @Parameter(description = "ID of the user to fetch", required = true) @PathVariable Long id) {
        if (!userRole.equals("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Admin role required.");
        }
        log.info("GET /users/{} - Admin request to fetch user", id);
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @Operation(
        summary = "Delete user",
        description = "Admin only — deletes a user by their ID. JWT is validated at the API gateway — the resolved role is forwarded via the X-User-Role header.",
        parameters = {
            @Parameter(
                name = "X-User-Role",
                in = ParameterIn.HEADER,
                required = true,
                description = "Authenticated user's role, injected by the API gateway. Must be ROLE_ADMIN.",
                schema = @Schema(type = "string", example = "ROLE_ADMIN")
            )
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden — Admin role required", content = @Content)
    })
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(
            @RequestHeader("X-User-Role") String userRole,
            @Parameter(description = "ID of the user to delete", required = true) @PathVariable Long id) {
        if (!userRole.equals("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Admin role required.");
        }
        log.info("DELETE /users/{} - Admin request to delete user", id);
        userService.deleteUser(id);
        log.info("DELETE /users/{} - User deleted successfully", id);
        return ResponseEntity.ok("User deleted successfully.");
    }

    @Operation(
        summary = "Search users",
        description = "Admin only — searches users by name or email keyword. JWT is validated at the API gateway — the resolved role is forwarded via the X-User-Role header.",
        parameters = {
            @Parameter(
                name = "X-User-Role",
                in = ParameterIn.HEADER,
                required = true,
                description = "Authenticated user's role, injected by the API gateway. Must be ROLE_ADMIN.",
                schema = @Schema(type = "string", example = "ROLE_ADMIN")
            )
        }
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results returned"),
        @ApiResponse(responseCode = "403", description = "Forbidden — Admin role required", content = @Content)
    })
    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(
            @RequestHeader("X-User-Role") String userRole,
            @Parameter(description = "Keyword to search by name or email", required = true) @RequestParam String keyword) {
        if (!userRole.equals("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied — Admin role required.");
        }
        log.info("GET /users/search - Admin search request with keyword: '{}'", keyword);
        return ResponseEntity.ok(userService.searchUsers(keyword));
    }
}