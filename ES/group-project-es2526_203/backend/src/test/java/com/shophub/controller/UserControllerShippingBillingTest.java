package com.shophub.controller;

import com.shophub.dto.ShippingBillingDTO;
import com.shophub.service.ShippingBillingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerShippingBillingTest {

    @Mock
    private ShippingBillingService shippingBillingService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private UserController userController;

    private String testUserId;
    private ShippingBillingDTO shippingBillingDTO;
    private ShippingBillingDTO.AddressInfo shippingAddress;
    private ShippingBillingDTO.AddressInfo billingAddress;

    @BeforeEach
    void setUp() {
        testUserId = "user-456";
        
        // Mock JWT
        when(jwt.getSubject()).thenReturn(testUserId);
        
        // Setup shipping address
        shippingAddress = ShippingBillingDTO.AddressInfo.builder()
                .fullName("John")
                .address("123 Main Street")
                .city("New York")
                .postalCode("10001")
                .phone("+1-555-0123")
                .build();
        
        // Setup billing address
        billingAddress = ShippingBillingDTO.AddressInfo.builder()
                .fullName("John")
                .address("456 Oak Avenue")
                .city("Brooklyn")
                .postalCode("11201")
                .phone("+1-555-0456")
                .build();
        
        // Complete DTO with both addresses
        shippingBillingDTO = ShippingBillingDTO.builder()
                .shipping(shippingAddress)
                .billing(billingAddress)
                .build();
    }

    /**
     * Test: Successfully update both shipping and billing information
     */
    @Test
    void updateShippingBilling_ShouldUpdateBothAddresses_WhenBothProvided() {
        // Given
        when(shippingBillingService.updateShippingBillingInfo(eq(testUserId), any(ShippingBillingDTO.class)))
                .thenReturn(shippingBillingDTO);

        // When
        ResponseEntity<ShippingBillingDTO> response = userController.updateShippingBilling(jwt, shippingBillingDTO);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.OK, response.getStatusCode(), "HTTP status should be OK");
        assertNotNull(response.getBody(), "Response body should not be null");
        
        ShippingBillingDTO result = response.getBody();
        
        // Verify shipping address
        assertNotNull(result.getShipping(), "Shipping address should not be null");
        assertEquals("John", result.getShipping().getFullName());
        assertEquals("123 Main Street", result.getShipping().getAddress());
        assertEquals("New York", result.getShipping().getCity());
        assertEquals("10001", result.getShipping().getPostalCode());
        assertEquals("+1-555-0123", result.getShipping().getPhone());
        
        // Verify billing address
        assertNotNull(result.getBilling(), "Billing address should not be null");
        assertEquals("John", result.getBilling().getFullName());
        assertEquals("456 Oak Avenue", result.getBilling().getAddress());
        assertEquals("Brooklyn", result.getBilling().getCity());
        assertEquals("11201", result.getBilling().getPostalCode());
        assertEquals("+1-555-0456", result.getBilling().getPhone());
        
        verify(shippingBillingService, times(1)).updateShippingBillingInfo(eq(testUserId), any(ShippingBillingDTO.class));
    }

    /**
     * Test: Update only shipping information
     */
    @Test
    void updateShippingBilling_ShouldUpdateShippingOnly_WhenOnlyShippingProvided() {
        // Given
        ShippingBillingDTO shippingOnlyDTO = ShippingBillingDTO.builder()
                .shipping(shippingAddress)
                .billing(null)
                .build();
        
        when(shippingBillingService.updateShippingBillingInfo(eq(testUserId), any(ShippingBillingDTO.class)))
                .thenReturn(shippingOnlyDTO);

        // When
        ResponseEntity<ShippingBillingDTO> response = userController.updateShippingBilling(jwt, shippingOnlyDTO);

        // Then
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getShipping(), "Shipping address should be set");
        assertEquals("123 Main Street", response.getBody().getShipping().getAddress());
        
        verify(shippingBillingService, times(1)).updateShippingBillingInfo(eq(testUserId), any(ShippingBillingDTO.class));
    }

    /**
     * Test: Update only billing information
     */
    @Test
    void updateShippingBilling_ShouldUpdateBillingOnly_WhenOnlyBillingProvided() {
        // Given
        ShippingBillingDTO billingOnlyDTO = ShippingBillingDTO.builder()
                .shipping(null)
                .billing(billingAddress)
                .build();
        
        when(shippingBillingService.updateShippingBillingInfo(eq(testUserId), any(ShippingBillingDTO.class)))
                .thenReturn(billingOnlyDTO);

        // When
        ResponseEntity<ShippingBillingDTO> response = userController.updateShippingBilling(jwt, billingOnlyDTO);

        // Then
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getBilling(), "Billing address should be set");
        assertEquals("456 Oak Avenue", response.getBody().getBilling().getAddress());
        
        verify(shippingBillingService, times(1)).updateShippingBillingInfo(eq(testUserId), any(ShippingBillingDTO.class));
    }

    /**
     * Test: Get existing shipping and billing information
     */
    @Test
    void getShippingBilling_ShouldReturnSavedAddresses_WhenAddressesExist() {
        // Given
        when(shippingBillingService.getShippingBillingInfo(testUserId))
                .thenReturn(shippingBillingDTO);

        // When
        ResponseEntity<ShippingBillingDTO> response = userController.getShippingBilling(jwt);

        // Then
        assertNotNull(response, "Response should not be null");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        ShippingBillingDTO result = response.getBody();
        assertNotNull(result.getShipping());
        assertNotNull(result.getBilling());
        assertEquals("John", result.getShipping().getFullName());
        assertEquals("John", result.getBilling().getFullName());
        
        verify(shippingBillingService, times(1)).getShippingBillingInfo(testUserId);
    }


    /**
     * Test: Delete shipping and billing information
     */
    @Test
    void deleteShippingBilling_ShouldDeleteAddresses_WhenAddressesExist() {
        // Given
        doNothing().when(shippingBillingService).deleteShippingBillingInfo(testUserId);

        // When
        ResponseEntity<Map<String, String>> response = userController.deleteShippingBilling(jwt);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Shipping and billing info deleted successfully", 
                     response.getBody().get("message"));
        
        verify(shippingBillingService, times(1)).deleteShippingBillingInfo(testUserId);
    }

    /**
     * Test: Verify all address fields are properly updated
     */
    @Test
    void updateShippingBilling_ShouldUpdateAllFields_WhenAllFieldsProvided() {
        // Given
        when(shippingBillingService.updateShippingBillingInfo(eq(testUserId), any(ShippingBillingDTO.class)))
                .thenReturn(shippingBillingDTO);

        // When
        ResponseEntity<ShippingBillingDTO> response = userController.updateShippingBilling(jwt, shippingBillingDTO);

        // Then
        ShippingBillingDTO result = response.getBody();
        
        // Verify all shipping fields are populated
        assertNotNull(result.getShipping().getFullName(), "Shipping full name should not be null");
        assertNotNull(result.getShipping().getAddress(), "Shipping address should not be null");
        assertNotNull(result.getShipping().getCity(), "Shipping city should not be null");
        assertNotNull(result.getShipping().getPostalCode(), "Shipping postal code should not be null");
        assertNotNull(result.getShipping().getPhone(), "Shipping phone should not be null");
        
        // Verify all billing fields are populated
        assertNotNull(result.getBilling().getFullName(), "Billing full name should not be null");
        assertNotNull(result.getBilling().getAddress(), "Billing address should not be null");
        assertNotNull(result.getBilling().getCity(), "Billing city should not be null");
        assertNotNull(result.getBilling().getPostalCode(), "Billing postal code should not be null");
        assertNotNull(result.getBilling().getPhone(), "Billing phone should not be null");
        
        verify(shippingBillingService, times(1)).updateShippingBillingInfo(eq(testUserId), any(ShippingBillingDTO.class));
    }
}
