package com.shophub.service;

import com.shophub.dto.UserDTO;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.User;
import com.shophub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId("user123")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .role("customer")
                .build();

        testUserDTO = UserDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .build();
    }

    @Test
    void getUserById_ShouldReturnUser() {
        // Given
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getUserById("user123");

        // Then
        assertEquals("user123", result.getUserId());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        verify(userRepository).findById("user123");
    }

    @Test
    void getUserById_ShouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findById("user123")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById("user123"));
        verify(userRepository).findById("user123");
    }

    @Test
    void getOrCreateUser_ShouldReturnExistingUser() {
        // Given
        when(jwt.getSubject()).thenReturn("user123");
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getOrCreateUser(jwt);

        // Then
        assertEquals("user123", result.getUserId());
        verify(userRepository).findById("user123");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getOrCreateUser_ShouldCreateNewUserWhenNotExists() {
        // Given
        when(jwt.getSubject()).thenReturn("user123");
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(jwt.getClaimAsString("given_name")).thenReturn("John");
        when(jwt.getClaimAsString("family_name")).thenReturn("Doe");
        when(jwt.getClaimAsString("name")).thenReturn("John Doe");
        when(userRepository.findById("user123")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.getOrCreateUser(jwt);

        // Then
        assertNotNull(result);
        verify(userRepository).findById("user123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserFromJwt_ShouldCreateUserWithJwtClaims() {
        // Given
        when(jwt.getSubject()).thenReturn("user123");
        when(jwt.getClaimAsString("email")).thenReturn("test@example.com");
        when(jwt.getClaimAsString("given_name")).thenReturn("John");
        when(jwt.getClaimAsString("family_name")).thenReturn("Doe");
        when(jwt.getClaimAsString("name")).thenReturn("John Doe");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.createUserFromJwt(jwt);

        // Then
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUserFromJwt_ShouldHandleMissingClaims() {
        // Given
        when(jwt.getSubject()).thenReturn("user123");
        when(jwt.getClaimAsString("email")).thenReturn(null);
        when(jwt.getClaimAsString("given_name")).thenReturn(null);
        when(jwt.getClaimAsString("family_name")).thenReturn(null);
        when(jwt.getClaimAsString("name")).thenReturn("John Doe");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.createUserFromJwt(jwt);

        // Then
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_ShouldUpdateUserProfile() {
        // Given
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateProfile("user123", testUserDTO);

        // Then
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("1234567890", result.getPhone());
        verify(userRepository).findById("user123");
        verify(userRepository).save(testUser);
    }

    @Test
    void updateProfile_ShouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findById("user123")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> userService.updateProfile("user123", testUserDTO));
        verify(userRepository).findById("user123");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getOrCreateUser_WithParameters_ShouldReturnExistingUser() {
        // Given
        when(userRepository.findById("user123")).thenReturn(Optional.of(testUser));

        // When
        User result = userService.getOrCreateUser("user123", "test@example.com", "John", "Doe");

        // Then
        assertEquals("user123", result.getUserId());
        verify(userRepository).findById("user123");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getOrCreateUser_WithParameters_ShouldCreateNewUser() {
        // Given
        when(userRepository.findById("user123")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.getOrCreateUser("user123", "test@example.com", "John", "Doe");

        // Then
        assertNotNull(result);
        verify(userRepository).findById("user123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void userExists_ShouldReturnTrueWhenUserExists() {
        // Given
        when(userRepository.existsByUserId("user123")).thenReturn(true);

        // When
        boolean result = userService.userExists("user123");

        // Then
        assertTrue(result);
        verify(userRepository).existsByUserId("user123");
    }

    @Test
    void userExists_ShouldReturnFalseWhenUserNotExists() {
        // Given
        when(userRepository.existsByUserId("user123")).thenReturn(false);

        // When
        boolean result = userService.userExists("user123");

        // Then
        assertFalse(result);
        verify(userRepository).existsByUserId("user123");
    }
}
