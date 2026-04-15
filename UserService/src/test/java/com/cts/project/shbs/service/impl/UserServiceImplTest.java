package com.cts.project.shbs.service.impl;

import com.cts.project.shbs.client.LoyaltyServiceClient;
import com.cts.project.shbs.dto.JwtResponse;
import com.cts.project.shbs.dto.LoginRequest;
import com.cts.project.shbs.dto.RegisterRequest;
import com.cts.project.shbs.exception.*;
import com.cts.project.shbs.model.Role;
import com.cts.project.shbs.model.User;
import com.cts.project.shbs.repository.UserRepository;
import com.cts.project.shbs.security.JwtUtils;
import com.cts.project.shbs.service.EmailService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtils jwtUtils;
    @Mock EmailService emailService;
    @Mock LoyaltyServiceClient loyaltyServiceClient;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks UserServiceImpl userService;

    private User guestUser;
    private User adminUser;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        guestUser = new User();
        guestUser.setUserId(1L);
        guestUser.setName("Alice");
        guestUser.setEmail("alice@example.com");
        guestUser.setPassword("encodedPass");
        guestUser.setRole(Role.ROLE_GUEST);
        guestUser.setContactNumber("9876543210");

        adminUser = new User();
        adminUser.setUserId(2L);
        adminUser.setName("Bob Admin");
        adminUser.setEmail("bob@example.com");
        adminUser.setPassword("encodedPass");
        adminUser.setRole(Role.ROLE_ADMIN);

        registerRequest = new RegisterRequest();
        registerRequest.setName("Alice");
        registerRequest.setEmail("alice@example.com");
        registerRequest.setPassword("plain123");
        registerRequest.setRole(Role.ROLE_GUEST);
        registerRequest.setContactNumber("9876543210");
        
        ReflectionTestUtils.setField(userService, "self", userService);
    }

    // ── registerUser ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("registerUser()")
    class RegisterUser {

        @Test
        @DisplayName("registers GUEST user and initializes loyalty account")
        void registerGuest_success() {
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(passwordEncoder.encode("plain123")).thenReturn("encodedPass");
            when(userRepository.save(any(User.class))).thenReturn(guestUser);

            User result = userService.registerUser(registerRequest);

            assertThat(result).isEqualTo(guestUser);
            verify(loyaltyServiceClient).initializeLoyaltyAccount(1L);
        }

        @Test
        @DisplayName("registers ADMIN user and skips loyalty initialization")
        void registerAdmin_noLoyalty() {
            registerRequest.setEmail("bob@example.com");
            registerRequest.setRole(Role.ROLE_ADMIN);

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("encodedPass");
            when(userRepository.save(any(User.class))).thenReturn(adminUser);

            userService.registerUser(registerRequest);

            verifyNoInteractions(loyaltyServiceClient);
        }

        @Test
        @DisplayName("throws EmailAlreadyExistsException when email is taken")
        void duplicateEmail_throws() {
            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> userService.registerUser(registerRequest))
                    .isInstanceOf(EmailAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("completes registration even when loyalty client throws")
        void loyaltyClientFails_doesNotAbortRegistration() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("encodedPass");
            when(userRepository.save(any())).thenReturn(guestUser);
            doThrow(new RuntimeException("Loyalty service down"))
                    .when(loyaltyServiceClient).initializeLoyaltyAccount(anyLong());

            assertThatCode(() -> userService.registerUser(registerRequest))
                    .doesNotThrowAnyException();

            verify(userRepository).save(any());
            verify(loyaltyServiceClient).initializeLoyaltyAccount(anyLong());
        }

        @Test
        @DisplayName("encodes password before saving")
        void passwordIsEncoded() {
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(passwordEncoder.encode("plain123")).thenReturn("encodedPass");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                assertThat(u.getPassword()).isEqualTo("encodedPass"); // raw password must not be saved
                return guestUser;
            });

            userService.registerUser(registerRequest);
        }
    }

    // ── loginUser ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("loginUser()")
    class LoginUser {

        private LoginRequest loginRequest;

        @BeforeEach
        void setUp() {
            loginRequest = new LoginRequest();
            loginRequest.setEmail("alice@example.com");
            loginRequest.setPassword("plain123");
        }

        @Test
        @DisplayName("returns JwtResponse on valid credentials")
        void success() {
            org.springframework.security.core.userdetails.User principal =
                    new org.springframework.security.core.userdetails.User("1", "encodedPass", List.of());
            Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtUtils.generateJwtToken(auth)).thenReturn("mock-jwt");
            when(userRepository.findById(1L)).thenReturn(Optional.of(guestUser));

            JwtResponse response = userService.loginUser(loginRequest);

            assertThat(response.getToken()).isEqualTo("mock-jwt");
            assertThat(response.getEmail()).isEqualTo("alice@example.com");
            assertThat(response.getRole()).isEqualTo("ROLE_GUEST");
            assertThat(response.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("throws BadCredentialsException on wrong password")
        void wrongPassword_throws() {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> userService.loginUser(loginRequest))
                    .isInstanceOf(BadCredentialsException.class);

            verifyNoInteractions(jwtUtils, userRepository);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user missing after auth")
        void userNotFoundAfterAuth() {
            org.springframework.security.core.userdetails.User principal =
                    new org.springframework.security.core.userdetails.User("99", "encodedPass", List.of());
            Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, List.of());

            when(authenticationManager.authenticate(any())).thenReturn(auth);
            when(jwtUtils.generateJwtToken(auth)).thenReturn("mock-jwt");
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.loginUser(loginRequest))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ── getUserById ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserById()")
    class GetUserById {

        @Test
        @DisplayName("returns user when found")
        void found() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(guestUser));

            User result = userService.getUserById(1L);

            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("throws UserNotFoundException when not found")
        void notFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(99L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ── getAllUsers ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllUsers()")
    class GetAllUsers {

        @Test
        @DisplayName("returns all users")
        void success() {
            when(userRepository.findAll()).thenReturn(List.of(guestUser, adminUser));

            assertThat(userService.getAllUsers()).hasSize(2);
        }

        @Test
        @DisplayName("returns empty list when no users exist")
        void empty() {
            when(userRepository.findAll()).thenReturn(List.of());

            assertThat(userService.getAllUsers()).isEmpty();
        }
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUser {

        @Test
        @DisplayName("deletes user when exists")
        void success() {
            when(userRepository.existsById(1L)).thenReturn(true);

            userService.deleteUser(1L);

            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("throws UserNotFoundException when user does not exist")
        void notFound() {
            when(userRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> userService.deleteUser(99L))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository, never()).deleteById(any());
        }
    }

    // ── updateUserProfile ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateUserProfile()")
    class UpdateUserProfile {

        @Test
        @DisplayName("updates name and contact number")
        void success() {
            RegisterRequest updateReq = new RegisterRequest();
            updateReq.setName("Alice Updated");
            updateReq.setContactNumber("1111111111");

            when(userRepository.findById(1L)).thenReturn(Optional.of(guestUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.updateUserProfile(1L, updateReq);

            assertThat(result.getName()).isEqualTo("Alice Updated");
            assertThat(result.getContactNumber()).isEqualTo("1111111111");
        }

        @Test
        @DisplayName("throws UserNotFoundException when user not found")
        void notFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserProfile(99L, registerRequest))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ── searchUsers ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("searchUsers()")
    class SearchUsers {

        @Test
        @DisplayName("returns matching users for a valid keyword")
        void withKeyword() {
            when(userRepository.searchByKeyword("alice")).thenReturn(List.of(guestUser));

            List<User> result = userService.searchUsers("alice");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getEmail()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("returns all users when keyword is null")
        void nullKeyword() {
            when(userRepository.findAll()).thenReturn(List.of(guestUser, adminUser));

            assertThat(userService.searchUsers(null)).hasSize(2);
            verify(userRepository, never()).searchByKeyword(any());
        }

        @Test
        @DisplayName("returns all users when keyword is blank")
        void blankKeyword() {
            when(userRepository.findAll()).thenReturn(List.of(guestUser, adminUser));

            assertThat(userService.searchUsers("   ")).hasSize(2);
            verify(userRepository, never()).searchByKeyword(any());
        }

        @Test
        @DisplayName("trims keyword before querying")
        void keywordIsTrimmed() {
            when(userRepository.searchByKeyword("alice")).thenReturn(List.of(guestUser));

            userService.searchUsers("  alice  ");

            verify(userRepository).searchByKeyword("alice"); // whitespace stripped
        }
    }

    // ── forgotPassword ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPassword {

        @Test
        @DisplayName("generates token and sends reset email")
        void success() {
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(guestUser));
            when(jwtUtils.generatePasswordResetToken(guestUser)).thenReturn("reset-token-xyz");

            userService.forgotPassword("alice@example.com");

            verify(emailService).sendPasswordResetEmail("alice@example.com", "reset-token-xyz");
        }

        @Test
        @DisplayName("throws AccountNotFoundException when email not registered")
        void emailNotFound() {
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.forgotPassword("unknown@example.com"))
                    .isInstanceOf(AccountNotFoundException.class);

            verifyNoInteractions(emailService);
        }
    }

    // ── resetPassword ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resetPassword()")
    class ResetPassword {

        @Test
        @DisplayName("resets password successfully with valid single-use token")
        void success() {
            Claims claims = mock(Claims.class);
            when(claims.getSubject()).thenReturn("1");
            when(claims.get("oldPasswordHash", String.class)).thenReturn("encodedPass");

            when(jwtUtils.validatePasswordResetToken("valid-token")).thenReturn(claims);
            when(userRepository.findById(1L)).thenReturn(Optional.of(guestUser));
            when(passwordEncoder.encode("newPass123")).thenReturn("newEncodedPass");

            userService.resetPassword("valid-token", "newPass123");

            assertThat(guestUser.getPassword()).isEqualTo("newEncodedPass");
            verify(userRepository).save(guestUser);
        }

        @Test
        @DisplayName("throws InvalidOrExpiredTokenException for bad JWT")
        void invalidToken() {
            when(jwtUtils.validatePasswordResetToken("bad-token"))
                    .thenThrow(new JwtException("expired"));

            assertThatThrownBy(() -> userService.resetPassword("bad-token", "newPass"))
                    .isInstanceOf(InvalidOrExpiredTokenException.class);
        }

        @Test
        @DisplayName("throws PasswordResetTokenAlreadyUsedException when password already changed")
        void tokenAlreadyUsed() {
            Claims claims = mock(Claims.class);
            when(claims.getSubject()).thenReturn("1");
            when(claims.get("oldPasswordHash", String.class)).thenReturn("differentOldHash");

            when(jwtUtils.validatePasswordResetToken("used-token")).thenReturn(claims);
            when(userRepository.findById(1L)).thenReturn(Optional.of(guestUser)); // password = "encodedPass"

            assertThatThrownBy(() -> userService.resetPassword("used-token", "newPass"))
                    .isInstanceOf(PasswordResetTokenAlreadyUsedException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws UserNotFoundException when user missing after token validation")
        void userNotFound() {
            Claims claims = mock(Claims.class);
            when(claims.getSubject()).thenReturn("99");

            when(jwtUtils.validatePasswordResetToken("orphan-token")).thenReturn(claims);
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.resetPassword("orphan-token", "newPass"))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}