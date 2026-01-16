package com.shophub.service;

import com.shophub.dto.ShippingBillingDTO;
import com.shophub.model.ShippingBillingInfo;
import com.shophub.repository.ShippingBillingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingBillingServiceTest {

    @Mock
    private ShippingBillingRepository shippingBillingRepository;

    @InjectMocks
    private ShippingBillingService shippingBillingService;

    private ShippingBillingInfo testInfo;
    private ShippingBillingDTO testDTO;

    @BeforeEach
    void setUp() {
        testInfo = ShippingBillingInfo.builder()
                .id(1L)
                .userId("user123")
                .shippingFullName("John Doe")
                .shippingAddress("123 Main St")
                .shippingCity("Test City")
                .shippingPostalCode("12345")
                .shippingPhone("1234567890")
                .billingFullName("John Doe")
                .billingAddress("123 Main St")
                .billingCity("Test City")
                .billingPostalCode("12345")
                .billingPhone("1234567890")
                .build();

        ShippingBillingDTO.AddressInfo shippingAddress = ShippingBillingDTO.AddressInfo.builder()
                .fullName("John Doe")
                .address("123 Main St")
                .city("Test City")
                .postalCode("12345")
                .phone("1234567890")
                .build();

        ShippingBillingDTO.AddressInfo billingAddress = ShippingBillingDTO.AddressInfo.builder()
                .fullName("John Doe")
                .address("123 Main St")
                .city("Test City")
                .postalCode("12345")
                .phone("1234567890")
                .build();

        testDTO = ShippingBillingDTO.builder()
                .shipping(shippingAddress)
                .billing(billingAddress)
                .build();
    }

    @Test
    void getShippingBillingInfo_ShouldReturnExistingInfo() {
        // Given
        when(shippingBillingRepository.findByUserId("user123")).thenReturn(Optional.of(testInfo));

        // When
        ShippingBillingDTO result = shippingBillingService.getShippingBillingInfo("user123");

        // Then
        assertNotNull(result);
        assertEquals("John Doe", result.getShipping().getFullName());
        assertEquals("123 Main St", result.getShipping().getAddress());
        assertEquals("Test City", result.getShipping().getCity());
        assertEquals("12345", result.getShipping().getPostalCode());
        assertEquals("1234567890", result.getShipping().getPhone());
        verify(shippingBillingRepository).findByUserId("user123");
    }

    @Test
    void getShippingBillingInfo_ShouldReturnEmptyDTOWhenNotFound() {
        // Given
        when(shippingBillingRepository.findByUserId("user123")).thenReturn(Optional.empty());

        // When
        ShippingBillingDTO result = shippingBillingService.getShippingBillingInfo("user123");

        // Then
        assertNotNull(result);
        assertEquals("", result.getShipping().getFullName());
        assertEquals("", result.getShipping().getAddress());
        assertEquals("", result.getShipping().getCity());
        assertEquals("", result.getShipping().getPostalCode());
        assertEquals("", result.getShipping().getPhone());
        verify(shippingBillingRepository).findByUserId("user123");
    }

    @Test
    void updateShippingBillingInfo_ShouldUpdateExistingInfo() {
        // Given
        when(shippingBillingRepository.findByUserId("user123")).thenReturn(Optional.of(testInfo));
        when(shippingBillingRepository.save(any(ShippingBillingInfo.class))).thenReturn(testInfo);

        // When
        ShippingBillingDTO result = shippingBillingService.updateShippingBillingInfo("user123", testDTO);

        // Then
        assertNotNull(result);
        assertEquals("John Doe", result.getShipping().getFullName());
        verify(shippingBillingRepository).findByUserId("user123");
        verify(shippingBillingRepository).save(testInfo);
    }

    @Test
    void updateShippingBillingInfo_ShouldCreateNewInfoWhenNotFound() {
        // Given
        when(shippingBillingRepository.findByUserId("user123")).thenReturn(Optional.empty());
        when(shippingBillingRepository.save(any(ShippingBillingInfo.class))).thenReturn(testInfo);

        // When
        ShippingBillingDTO result = shippingBillingService.updateShippingBillingInfo("user123", testDTO);

        // Then
        assertNotNull(result);
        verify(shippingBillingRepository).findByUserId("user123");
        verify(shippingBillingRepository).save(any(ShippingBillingInfo.class));
    }

    @Test
    void updateShippingBillingInfo_ShouldHandlePartialUpdates() {
        // Given
        ShippingBillingDTO partialDTO = ShippingBillingDTO.builder()
                .shipping(ShippingBillingDTO.AddressInfo.builder()
                        .fullName("Jane Doe")
                        .address("456 Oak St")
                        .city("New City")
                        .postalCode("54321")
                        .phone("9876543210")
                        .build())
                .billing(null) // Only shipping info provided
                .build();

        when(shippingBillingRepository.findByUserId("user123")).thenReturn(Optional.of(testInfo));
        when(shippingBillingRepository.save(any(ShippingBillingInfo.class))).thenReturn(testInfo);

        // When
        ShippingBillingDTO result = shippingBillingService.updateShippingBillingInfo("user123", partialDTO);

        // Then
        assertNotNull(result);
        verify(shippingBillingRepository).findByUserId("user123");
        verify(shippingBillingRepository).save(testInfo);
    }

    @Test
    void deleteShippingBillingInfo_ShouldDeleteInfo() {
        // When
        shippingBillingService.deleteShippingBillingInfo("user123");

        // Then
        verify(shippingBillingRepository).deleteByUserId("user123");
    }
}
