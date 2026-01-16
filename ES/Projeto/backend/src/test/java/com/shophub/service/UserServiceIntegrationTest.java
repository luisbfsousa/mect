package com.shophub.service;

import com.shophub.dto.UserDTO;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.User;
import com.shophub.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void cleanup() {
        userRepository.deleteAll();
    }

    private Jwt createTestJwt(String userId, String email, String givenName, String familyName) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", userId)
                .claim("email", email)
                .claim("given_name", givenName)
                .claim("family_name", familyName)
                .claim("name", givenName + " " + familyName)
                .build();
    }

    @Test
    @Transactional
    void getUserById_retrievesExistingUser() {
        // Arrange
        User user = User.builder()
                .userId("test-user-1")
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .role("customer")
                .build();
        userRepository.save(user);

        // Act
        User retrieved = userService.getUserById("test-user-1");

        // Assert
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getUserId()).isEqualTo("test-user-1");
        assertThat(retrieved.getEmail()).isEqualTo("test@example.com");
        assertThat(retrieved.getFirstName()).isEqualTo("John");
        assertThat(retrieved.getLastName()).isEqualTo("Doe");
    }

    @Test
    @Transactional
    void getUserById_throwsExceptionWhenNotFound() {
        // Act and Assert
        assertThatThrownBy(() -> userService.getUserById("non-existent-user"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @Transactional
    void createUserFromJwt_createsNewUser() {
        // Arrange
        Jwt jwt = createTestJwt("jwt-user-1", "jwt@example.com", "Alice", "Smith");

        // Act
        User created = userService.createUserFromJwt(jwt);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getUserId()).isEqualTo("jwt-user-1");
        assertThat(created.getEmail()).isEqualTo("jwt@example.com");
        assertThat(created.getFirstName()).isEqualTo("Alice");
        assertThat(created.getLastName()).isEqualTo("Smith");
        assertThat(created.getRole()).isEqualTo("customer");
        assertThat(created.getCreatedAt()).isNotNull();

        // Verify in database
        Optional<User> fromDb = userRepository.findById("jwt-user-1");
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getEmail()).isEqualTo("jwt@example.com");
    }

    @Test
    @Transactional
    void createUserFromJwt_withMissingEmail_usesDefault() {
        // Arrange - JWT without email claim
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", "no-email-user")
                .claim("given_name", "NoEmail")
                .claim("family_name", "User")
                .build();

        // Act
        User created = userService.createUserFromJwt(jwt);

        // Assert
        assertThat(created.getEmail()).isEqualTo("no-email@example.com");
        assertThat(created.getFirstName()).isEqualTo("NoEmail");
    }

    @Test
    @Transactional
    void createUserFromJwt_withMissingNames_usesDefaults() {
        // Arrange - JWT with only email
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", "minimal-user")
                .claim("email", "minimal@example.com")
                .build();

        // Act
        User created = userService.createUserFromJwt(jwt);

        // Assert
        assertThat(created.getEmail()).isEqualTo("minimal@example.com");
        assertThat(created.getFirstName()).isEqualTo("Customer"); // Default
        assertThat(created.getLastName()).isEmpty();
    }

    @Test
    @Transactional
    void getOrCreateUser_returnsExistingUser() {
        // Arrange - create user first
        User existing = User.builder()
                .userId("existing-user")
                .email("existing@example.com")
                .firstName("Existing")
                .lastName("User")
                .role("customer")
                .build();
        userRepository.save(existing);

        Jwt jwt = createTestJwt("existing-user", "newemail@example.com", "NewName", "NewLastName");

        // Act
        User result = userService.getOrCreateUser(jwt);

        // Assert
        assertThat(result.getUserId()).isEqualTo("existing-user");
        assertThat(result.getEmail()).isEqualTo("existing@example.com"); // Original email preserved
        assertThat(result.getFirstName()).isEqualTo("Existing"); // Original name preserved

        // Verify only one user exists
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @Transactional
    void getOrCreateUser_createsNewUserWhenNotExists() {
        // Arrange
        Jwt jwt = createTestJwt("new-user", "new@example.com", "New", "User");

        // Act
        User result = userService.getOrCreateUser(jwt);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("new-user");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
        assertThat(result.getFirstName()).isEqualTo("New");
        assertThat(result.getLastName()).isEqualTo("User");

        // Verify user was persisted
        assertThat(userRepository.existsById("new-user")).isTrue();
    }

    @Test
    @Transactional
    void getOrCreateUserWithParams_createsNewUser() {
        // Act
        User result = userService.getOrCreateUser(
                "param-user",
                "param@example.com",
                "Param",
                "User"
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("param-user");
        assertThat(result.getEmail()).isEqualTo("param@example.com");
        assertThat(result.getFirstName()).isEqualTo("Param");
        assertThat(result.getLastName()).isEqualTo("User");
        assertThat(result.getRole()).isEqualTo("customer");

        // Verify in database
        Optional<User> fromDb = userRepository.findById("param-user");
        assertThat(fromDb).isPresent();
    }

    @Test
    @Transactional
    void getOrCreateUserWithParams_returnsExistingUser() {
        // Arrange
        User existing = userRepository.save(User.builder()
                .userId("existing-param-user")
                .email("original@example.com")
                .firstName("Original")
                .lastName("Name")
                .role("customer")
                .build());

        // Act
        User result = userService.getOrCreateUser(
                "existing-param-user",
                "new@example.com",
                "New",
                "Name"
        );

        // Assert
        assertThat(result.getUserId()).isEqualTo("existing-param-user");
        assertThat(result.getEmail()).isEqualTo("original@example.com"); // Original preserved
        assertThat(result.getFirstName()).isEqualTo("Original"); // Original preserved
    }

    @Test
    @Transactional
    void updateProfile_updatesUserFields() {
        // Arrange
        User user = User.builder()
                .userId("update-user")
                .email("update@example.com")
                .firstName("Old First")
                .lastName("Old Last")
                .phone(null)
                .role("customer")
                .build();
        userRepository.save(user);

        UserDTO updateDTO = UserDTO.builder()
                .firstName("New First")
                .lastName("New Last")
                .phone("+1234567890")
                .build();

        // Act
        User updated = userService.updateProfile("update-user", updateDTO);

        // Assert
        assertThat(updated.getFirstName()).isEqualTo("New First");
        assertThat(updated.getLastName()).isEqualTo("New Last");
        assertThat(updated.getPhone()).isEqualTo("+1234567890");
        assertThat(updated.getEmail()).isEqualTo("update@example.com"); // Email unchanged

        // Verify in database
        User fromDb = userRepository.findById("update-user").orElseThrow();
        assertThat(fromDb.getFirstName()).isEqualTo("New First");
        assertThat(fromDb.getPhone()).isEqualTo("+1234567890");
    }

    @Test
    @Transactional
    void updateProfile_throwsExceptionWhenUserNotFound() {
        // Arrange
        UserDTO updateDTO = UserDTO.builder()
                .firstName("Test")
                .lastName("User")
                .build();

        // Act and Assert
        assertThatThrownBy(() -> userService.updateProfile("non-existent", updateDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @Transactional
    void userExists_returnsTrueWhenUserExists() {
        // Arrange
        userRepository.save(User.builder()
                .userId("existing-check")
                .email("check@example.com")
                .firstName("Check")
                .lastName("User")
                .role("customer")
                .build());

        // Act
        boolean exists = userService.userExists("existing-check");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @Transactional
    void userExists_returnsFalseWhenUserDoesNotExist() {
        // Act
        boolean exists = userService.userExists("non-existent");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @Transactional
    void createMultipleUsers_allPersisted() {
        // Arrange and Act
        Jwt jwt1 = createTestJwt("user-1", "user1@example.com", "User", "One");
        Jwt jwt2 = createTestJwt("user-2", "user2@example.com", "User", "Two");
        Jwt jwt3 = createTestJwt("user-3", "user3@example.com", "User", "Three");

        User user1 = userService.createUserFromJwt(jwt1);
        User user2 = userService.createUserFromJwt(jwt2);
        User user3 = userService.createUserFromJwt(jwt3);

        // Assert
        assertThat(userRepository.count()).isEqualTo(3);
        assertThat(userRepository.findById("user-1")).isPresent();
        assertThat(userRepository.findById("user-2")).isPresent();
        assertThat(userRepository.findById("user-3")).isPresent();
    }

    @Test
    @Transactional
    void updateProfile_canClearPhone() {
        // Arrange
        User user = userRepository.save(User.builder()
                .userId("phone-user")
                .email("phone@example.com")
                .firstName("Phone")
                .lastName("User")
                .phone("+1234567890")
                .role("customer")
                .build());

        UserDTO updateDTO = UserDTO.builder()
                .firstName("Phone")
                .lastName("User")
                .phone(null) // Clear phone
                .build();

        // Act
        User updated = userService.updateProfile("phone-user", updateDTO);

        // Assert
        assertThat(updated.getPhone()).isNull();
    }

    @Test
    @Transactional
    void createUserFromJwt_setsDefaultRole() {
        // Arrange
        Jwt jwt = createTestJwt("role-user", "role@example.com", "Role", "Test");

        // Act
        User created = userService.createUserFromJwt(jwt);

        // Assert
        assertThat(created.getRole()).isEqualTo("customer");
    }

    @Test
    @Transactional
    void createUserFromJwt_withNameClaimOnly_usesNameForFirstName() {
        // Arrange - JWT with 'name' but no 'given_name'
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("sub", "name-only-user")
                .claim("email", "nameonly@example.com")
                .claim("name", "Full Name Here")
                .build();

        // Act
        User created = userService.createUserFromJwt(jwt);

        // Assert
        assertThat(created.getFirstName()).isEqualTo("Full Name Here");
    }
}
