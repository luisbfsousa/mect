package com.shophub.service;

import com.shophub.dto.UserDTO;
import com.shophub.exception.ResourceNotFoundException;
import com.shophub.model.User;
import com.shophub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private static final List<String> ROLE_PRIORITY = List.of(
            "administrator",
            "warehouse-staff",
            "content-manager",
            "customer"
    );
    
    @Transactional(readOnly = true)
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    @Transactional(readOnly = true)
    public List<User> getAllCustomers() {
        return userRepository.findByRoleIn(List.of("customer"));
    }
    
    @Transactional
    public User getOrCreateUser(Jwt jwt) {
        String userId = jwt.getSubject();

        return userRepository.findById(userId)
                .map(existing -> updateRoleIfHigherPrivilege(existing, jwt))
                .orElseGet(() -> createUserFromJwt(jwt));
    }
    
    @Transactional
    public User createUserFromJwt(Jwt jwt) {
        String userId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String givenName = jwt.getClaimAsString("given_name");
        String familyName = jwt.getClaimAsString("family_name");
        String name = jwt.getClaimAsString("name");

        // Extract role from Keycloak JWT
        String role = resolveRoleFromJwt(jwt);

        User user = User.builder()
                .userId(userId)
                .email(email != null ? email : "no-email@example.com")
                .firstName(givenName != null ? givenName : (name != null ? name : "Customer"))
                .lastName(familyName != null ? familyName : "")
                .role("customer")
                .isLocked(false)
                .isDeactivated(false)
                .build();

        log.info("Creating new user: {} with role: {}", userId, role);
        return userRepository.save(user);
    }
    
    @Transactional
    public User updateProfile(String userId, UserDTO userDTO) {
        User user = getUserById(userId);
        
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPhone(userDTO.getPhone());
        
        return userRepository.save(user);
    }
    
    @Transactional
    public User updateCustomerDetails(String userId, String firstName, String lastName, String email, String phone) {
        User user = getUserById(userId);
        
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhone(phone);
        
        log.info("Updated customer details for user: {}", userId);
        return userRepository.save(user);
    }
    
    @Transactional
    public User lockAccount(String userId) {
        User user = getUserById(userId);
        user.setIsLocked(true);
        log.info("Locked account for user: {}", userId);
        return userRepository.save(user);
    }
    
    @Transactional
    public User unlockAccount(String userId) {
        User user = getUserById(userId);
        user.setIsLocked(false);
        log.info("Unlocked account for user: {}", userId);
        return userRepository.save(user);
    }
    
    @Transactional
    public User deactivateAccount(String userId) {
        User user = getUserById(userId);
        user.setIsDeactivated(true);
        log.info("Deactivated account for user: {}", userId);
        return userRepository.save(user);
    }
    
    @Transactional
    public User reactivateAccount(String userId) {
        User user = getUserById(userId);
        user.setIsDeactivated(false);
        log.info("Reactivated account for user: {}", userId);
        return userRepository.save(user);
    }
    
    public User getOrCreateUser(String userId, String email, String firstName, String lastName) {
    return userRepository.findById(userId)
        .orElseGet(() -> {
            User newUser = User.builder()
                .userId(userId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .role("customer")
                .isLocked(false)
                .isDeactivated(false)
                .build();
            User savedUser = userRepository.save(newUser);
            log.info("Created new user from Keycloak: {}", email);
            return savedUser;
        });
   }
    
    @Transactional(readOnly = true)
    public boolean userExists(String userId) {
        return userRepository.existsByUserId(userId);
    }

    private User updateRoleIfHigherPrivilege(User user, Jwt jwt) {
        String resolved = resolveRoleFromJwt(jwt);
        String current = normalizeRole(user.getRole());

        if (resolved == null) {
            return user;
        }

        int currentPriority = rolePriority(current);
        int newPriority = rolePriority(resolved);

        if (newPriority < currentPriority) {
            log.info("Upgrading role for user {} from {} to {}", user.getUserId(), current, resolved);
            user.setRole(resolved);
            return userRepository.save(user);
        }

        return user;
    }

    private String resolveRoleFromJwt(Jwt jwt) {
        String userId = jwt.getSubject();
        try {
            java.util.Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null && realmAccess.get("roles") != null) {
                @SuppressWarnings("unchecked")
                java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");

                List<String> normalizedRoles = roles.stream()
                        .flatMap(r -> Stream.of(r, r.replace("_", "-")))
                        .map(this::normalizeRole)
                        .distinct()
                        .collect(Collectors.toList());

                return ROLE_PRIORITY.stream()
                        .filter(normalizedRoles::contains)
                        .findFirst()
                        .orElse("customer");
            }
        } catch (Exception e) {
            log.warn("Could not extract role from JWT for user {}: {}", userId, e.getMessage());
        }
        return "customer";
    }

    private String normalizeRole(String role) {
        return role == null ? "customer" : role.toLowerCase().replace("_", "-");
    }

    private int rolePriority(String role) {
        int idx = ROLE_PRIORITY.indexOf(normalizeRole(role));
        return idx >= 0 ? idx : ROLE_PRIORITY.size();
    }
}
