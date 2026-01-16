package com.shophub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shophub.config.TestSecurityConfig;
import com.shophub.dto.ShippingBillingDTO;
import com.shophub.dto.UserDTO;
import com.shophub.model.User;
import com.shophub.repository.ShippingBillingRepository;
import com.shophub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class UserControllerFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShippingBillingRepository shippingBillingRepository;

    private String testUserId = "test-user-123";

    @BeforeEach
    void setUp() {
        // Clean up
        shippingBillingRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @Transactional
    void getProfile_returnsUserProfile_whenUserExists() throws Exception {
        // Arrange
        User user = User.builder()
                .userId(testUserId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .phone("555-1234")
                .role("customer")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        // Act and Assert
        mockMvc.perform(get("/api/profile")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)
                                .claim("email", "test@example.com")
                                .claim("given_name", "John")
                                .claim("family_name", "Doe"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id", is(testUserId)))
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.first_name", is("John")))
                .andExpect(jsonPath("$.last_name", is("Doe")));
    }

    @Test
    @Transactional
    void getProfile_createsUser_whenUserDoesNotExist() throws Exception {
        // Act and Assert - User should be created automatically
        mockMvc.perform(get("/api/profile")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)
                                .claim("email", "newuser@example.com")
                                .claim("given_name", "Jane")
                                .claim("family_name", "Smith"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id", is(testUserId)))
                .andExpect(jsonPath("$.email", is("newuser@example.com")));
    }

    @Test
    @Transactional
    void updateProfile_updatesUserInfo_withValidData() throws Exception {
        // Arrange
        User user = User.builder()
                .userId(testUserId)
                .email("test@example.com")
                .firstName("Old")
                .lastName("Name")
                .role("customer")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userRepository.save(user);

        UserDTO updateDTO = UserDTO.builder()
                .email("test@example.com") // Email cannot be changed
                .firstName("New")
                .lastName("Updated")
                .phone("555-9999")
                .build();

        String updateJson = objectMapper.writeValueAsString(updateDTO);

        // Act and Assert
        mockMvc.perform(put("/api/profile")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("test@example.com"))) // Email stays the same
                .andExpect(jsonPath("$.first_name", is("New")))
                .andExpect(jsonPath("$.last_name", is("Updated")))
                .andExpect(jsonPath("$.phone", is("555-9999")));
    }

    @Test
    @Transactional
    void updateProfile_returns400_whenEmailIsMissing() throws Exception {
        // Arrange
        UserDTO updateDTO = UserDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .build();

        String updateJson = objectMapper.writeValueAsString(updateDTO);

        // Act and Assert
        mockMvc.perform(put("/api/profile")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void getShippingBilling_returnsEmptyInfo_whenNotSet() throws Exception {
        // Act and Assert - Returns empty address objects
        mockMvc.perform(get("/api/profile/shipping-billing")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipping").exists())
                .andExpect(jsonPath("$.shipping.fullName", is("")))
                .andExpect(jsonPath("$.shipping.address", is("")));
    }

    @Test
    @Transactional
    void updateShippingBilling_savesInfo_withValidData() throws Exception {
        // Arrange
        ShippingBillingDTO.AddressInfo shippingAddress = ShippingBillingDTO.AddressInfo.builder()
                .fullName("John Doe")
                .address("123 Main St")
                .city("Boston")
                .postalCode("02101")
                .phone("555-1234")
                .build();

        ShippingBillingDTO.AddressInfo billingAddress = ShippingBillingDTO.AddressInfo.builder()
                .fullName("John Doe")
                .address("456 Oak Ave")
                .city("Cambridge")
                .postalCode("02139")
                .phone("555-5678")
                .build();

        ShippingBillingDTO dto = ShippingBillingDTO.builder()
                .shipping(shippingAddress)
                .billing(billingAddress)
                .build();

        String dtoJson = objectMapper.writeValueAsString(dto);

        // Act and Assert
        mockMvc.perform(put("/api/profile/shipping-billing")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)
                                .claim("email", "test@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipping.fullName", is("John Doe")))
                .andExpect(jsonPath("$.shipping.address", is("123 Main St")))
                .andExpect(jsonPath("$.billing.fullName", is("John Doe")))
                .andExpect(jsonPath("$.billing.address", is("456 Oak Ave")));
    }

    @Test
    @Transactional
    void updateShippingBilling_updatesExistingInfo() throws Exception {
        // Arrange - First create shipping/billing info
        ShippingBillingDTO.AddressInfo initialAddress = ShippingBillingDTO.AddressInfo.builder()
                .fullName("Old Name")
                .address("Old Address")
                .city("Old City")
                .postalCode("00000")
                .phone("000-0000")
                .build();

        ShippingBillingDTO initialDto = ShippingBillingDTO.builder()
                .shipping(initialAddress)
                .build();

        String initialJson = objectMapper.writeValueAsString(initialDto);

        mockMvc.perform(put("/api/profile/shipping-billing")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)
                                .claim("email", "test@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(initialJson))
                .andExpect(status().isOk());

        // Update with new info
        ShippingBillingDTO.AddressInfo newAddress = ShippingBillingDTO.AddressInfo.builder()
                .fullName("New Name")
                .address("New Address")
                .city("New City")
                .postalCode("11111")
                .phone("111-1111")
                .build();

        ShippingBillingDTO newDto = ShippingBillingDTO.builder()
                .shipping(newAddress)
                .build();

        String newJson = objectMapper.writeValueAsString(newDto);

        // Act and Assert
        mockMvc.perform(put("/api/profile/shipping-billing")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)
                                .claim("email", "test@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipping.fullName", is("New Name")))
                .andExpect(jsonPath("$.shipping.address", is("New Address")));
    }

    @Test
    @Transactional
    void deleteShippingBilling_deletesInfo_whenExists() throws Exception {
        // Arrange - Create shipping/billing info first
        ShippingBillingDTO.AddressInfo address = ShippingBillingDTO.AddressInfo.builder()
                .fullName("John Doe")
                .address("123 Main St")
                .city("Boston")
                .postalCode("02101")
                .phone("555-1234")
                .build();

        ShippingBillingDTO dto = ShippingBillingDTO.builder()
                .shipping(address)
                .build();

        String dtoJson = objectMapper.writeValueAsString(dto);

        mockMvc.perform(put("/api/profile/shipping-billing")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId)
                                .claim("email", "test@example.com")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(dtoJson))
                .andExpect(status().isOk());

        // Act - Delete the info
        mockMvc.perform(delete("/api/profile/shipping-billing")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Shipping and billing info deleted successfully")));

        // Assert - Info should be cleared (empty objects)
        mockMvc.perform(get("/api/profile/shipping-billing")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shipping.fullName", is("")))
                .andExpect(jsonPath("$.shipping.address", is("")));
    }

    @Test
    @Transactional
    void deleteShippingBilling_succeeds_whenNoInfoExists() throws Exception {
        // Act and Assert - Should not fail even if nothing to delete
        mockMvc.perform(delete("/api/profile/shipping-billing")
                        .with(jwt().jwt(jwt -> jwt.subject(testUserId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Shipping and billing info deleted successfully")));
    }
}
