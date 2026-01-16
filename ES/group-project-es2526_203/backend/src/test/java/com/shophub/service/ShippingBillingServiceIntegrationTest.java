package com.shophub.service;

import com.shophub.dto.ShippingBillingDTO;
import com.shophub.model.ShippingBillingInfo;
import com.shophub.repository.ShippingBillingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class ShippingBillingServiceIntegrationTest {

    @Autowired
    private ShippingBillingService shippingBillingService;

    @Autowired
    private ShippingBillingRepository shippingBillingRepository;

    @AfterEach
    void cleanup() {
        shippingBillingRepository.deleteAll();
    }

    @Test
    @Transactional
    void getShippingBillingInfo_returnsEmptyDTOWhenNotFound() {
        // Act
        ShippingBillingDTO result = shippingBillingService.getShippingBillingInfo("user-123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getShipping()).isNotNull();
        assertThat(result.getBilling()).isNotNull();
        assertThat(result.getShipping().getFullName()).isEmpty();
        assertThat(result.getShipping().getAddress()).isEmpty();
        assertThat(result.getBilling().getFullName()).isEmpty();
    }

    @Test
    @Transactional
    void getShippingBillingInfo_returnsExistingInfo() {
        // Arrange
        ShippingBillingInfo info = ShippingBillingInfo.builder()
                .userId("user-123")
                .shippingFullName("John Doe")
                .shippingAddress("123 Main St")
                .shippingCity("New York")
                .shippingPostalCode("10001")
                .shippingPhone("+1234567890")
                .billingFullName("Jane Doe")
                .billingAddress("456 Broadway")
                .billingCity("Boston")
                .billingPostalCode("02101")
                .billingPhone("+0987654321")
                .build();
        shippingBillingRepository.save(info);

        // Act
        ShippingBillingDTO result = shippingBillingService.getShippingBillingInfo("user-123");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getShipping().getFullName()).isEqualTo("John Doe");
        assertThat(result.getShipping().getAddress()).isEqualTo("123 Main St");
        assertThat(result.getShipping().getCity()).isEqualTo("New York");
        assertThat(result.getShipping().getPostalCode()).isEqualTo("10001");
        assertThat(result.getShipping().getPhone()).isEqualTo("+1234567890");
        
        assertThat(result.getBilling().getFullName()).isEqualTo("Jane Doe");
        assertThat(result.getBilling().getAddress()).isEqualTo("456 Broadway");
        assertThat(result.getBilling().getCity()).isEqualTo("Boston");
        assertThat(result.getBilling().getPostalCode()).isEqualTo("02101");
        assertThat(result.getBilling().getPhone()).isEqualTo("+0987654321");
    }

    @Test
    @Transactional
    void updateShippingBillingInfo_createsNewInfoWhenNotExists() {
        // Arrange
        ShippingBillingDTO dto = ShippingBillingDTO.builder()
                .shipping(ShippingBillingDTO.AddressInfo.builder()
                        .fullName("Alice Smith")
                        .address("789 Park Ave")
                        .city("Chicago")
                        .postalCode("60601")
                        .phone("+1122334455")
                        .build())
                .billing(ShippingBillingDTO.AddressInfo.builder()
                        .fullName("Bob Smith")
                        .address("321 Lake St")
                        .city("Seattle")
                        .postalCode("98101")
                        .phone("+5544332211")
                        .build())
                .build();

        // Act
        ShippingBillingDTO result = shippingBillingService.updateShippingBillingInfo("user-new", dto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getShipping().getFullName()).isEqualTo("Alice Smith");
        assertThat(result.getShipping().getAddress()).isEqualTo("789 Park Ave");
        assertThat(result.getBilling().getFullName()).isEqualTo("Bob Smith");
        assertThat(result.getBilling().getCity()).isEqualTo("Seattle");

        // Verify in database
        Optional<ShippingBillingInfo> fromDb = shippingBillingRepository.findByUserId("user-new");
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getShippingFullName()).isEqualTo("Alice Smith");
        assertThat(fromDb.get().getBillingCity()).isEqualTo("Seattle");
    }

    @Test
    @Transactional
    void updateShippingBillingInfo_updatesExistingInfo() {
        // Arrange - create existing info
        ShippingBillingInfo existing = ShippingBillingInfo.builder()
                .userId("user-456")
                .shippingFullName("Old Name")
                .shippingAddress("Old Address")
                .shippingCity("Old City")
                .shippingPostalCode("00000")
                .shippingPhone("+0000000000")
                .billingFullName("Old Billing Name")
                .billingAddress("Old Billing Address")
                .billingCity("Old Billing City")
                .billingPostalCode("11111")
                .billingPhone("+1111111111")
                .build();
        shippingBillingRepository.save(existing);

        // Create update DTO
        ShippingBillingDTO updateDTO = ShippingBillingDTO.builder()
                .shipping(ShippingBillingDTO.AddressInfo.builder()
                        .fullName("Updated Name")
                        .address("Updated Address")
                        .city("Updated City")
                        .postalCode("99999")
                        .phone("+9999999999")
                        .build())
                .billing(ShippingBillingDTO.AddressInfo.builder()
                        .fullName("Updated Billing")
                        .address("Updated Billing Address")
                        .city("Updated Billing City")
                        .postalCode("88888")
                        .phone("+8888888888")
                        .build())
                .build();

        // Act
        ShippingBillingDTO result = shippingBillingService.updateShippingBillingInfo("user-456", updateDTO);

        // Assert
        assertThat(result.getShipping().getFullName()).isEqualTo("Updated Name");
        assertThat(result.getShipping().getAddress()).isEqualTo("Updated Address");
        assertThat(result.getShipping().getCity()).isEqualTo("Updated City");
        assertThat(result.getBilling().getFullName()).isEqualTo("Updated Billing");
        assertThat(result.getBilling().getPostalCode()).isEqualTo("88888");

        // Verify in database - should be only one record (updated, not new)
        Optional<ShippingBillingInfo> fromDb = shippingBillingRepository.findByUserId("user-456");
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getId()).isEqualTo(existing.getId()); // Same ID
        assertThat(fromDb.get().getShippingFullName()).isEqualTo("Updated Name");
    }

    @Test
    @Transactional
    void updateShippingBillingInfo_onlyShippingProvided_updatesBillingToNull() {
        // Arrange
        ShippingBillingDTO dto = ShippingBillingDTO.builder()
                .shipping(ShippingBillingDTO.AddressInfo.builder()
                        .fullName("Shipping Only")
                        .address("Shipping Address")
                        .city("Shipping City")
                        .postalCode("12345")
                        .phone("+1234567890")
                        .build())
                .build(); // No billing info

        // Act
        ShippingBillingDTO result = shippingBillingService.updateShippingBillingInfo("user-shipping-only", dto);

        // Assert
        assertThat(result.getShipping().getFullName()).isEqualTo("Shipping Only");
        
        // Verify in database
        Optional<ShippingBillingInfo> fromDb = shippingBillingRepository.findByUserId("user-shipping-only");
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getShippingFullName()).isEqualTo("Shipping Only");
        // Billing should be null when not provided
        assertThat(fromDb.get().getBillingFullName()).isNull();
    }

    @Test
    @Transactional
    void updateShippingBillingInfo_onlyBillingProvided_updatesShippingToNull() {
        // Arrange
        ShippingBillingDTO dto = ShippingBillingDTO.builder()
                .billing(ShippingBillingDTO.AddressInfo.builder()
                        .fullName("Billing Only")
                        .address("Billing Address")
                        .city("Billing City")
                        .postalCode("54321")
                        .phone("+0987654321")
                        .build())
                .build();

        // Act
        ShippingBillingDTO result = shippingBillingService.updateShippingBillingInfo("user-billing-only", dto);

        // Assert
        assertThat(result.getBilling().getFullName()).isEqualTo("Billing Only");
        
        // Verify in database
        Optional<ShippingBillingInfo> fromDb = shippingBillingRepository.findByUserId("user-billing-only");
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getBillingFullName()).isEqualTo("Billing Only");
        // Shipping should be null when not provided
        assertThat(fromDb.get().getShippingFullName()).isNull();
    }

    @Test
    @Transactional
    void deleteShippingBillingInfo_removesInfoFromDatabase() {
        // Arrange
        ShippingBillingInfo info = ShippingBillingInfo.builder()
                .userId("user-to-delete")
                .shippingFullName("Test User")
                .shippingAddress("Test Address")
                .shippingCity("Test City")
                .shippingPostalCode("00000")
                .shippingPhone("+0000000000")
                .build();
        shippingBillingRepository.save(info);

        // Verify exists
        assertThat(shippingBillingRepository.findByUserId("user-to-delete")).isPresent();

        // Act
        shippingBillingService.deleteShippingBillingInfo("user-to-delete");

        // Assert
        assertThat(shippingBillingRepository.findByUserId("user-to-delete")).isEmpty();
    }

    @Test
    @Transactional
    void deleteShippingBillingInfo_doesNotThrowWhenNotExists() {
        // Act - delete non-existent info (should not throw exception)
        shippingBillingService.deleteShippingBillingInfo("non-existent-user");

        // Assert
        assertThat(shippingBillingRepository.findByUserId("non-existent-user")).isEmpty();
    }

    @Test
    @Transactional
    void updateShippingBillingInfo_partialUpdate_preservesOtherFields() {
        // Arrange - create initial info with both shipping and billing
        ShippingBillingInfo initial = ShippingBillingInfo.builder()
                .userId("user-partial")
                .shippingFullName("Initial Shipping")
                .shippingAddress("Initial Shipping Address")
                .shippingCity("Initial City")
                .shippingPostalCode("11111")
                .shippingPhone("+1111111111")
                .billingFullName("Initial Billing")
                .billingAddress("Initial Billing Address")
                .billingCity("Initial Billing City")
                .billingPostalCode("22222")
                .billingPhone("+2222222222")
                .build();
        shippingBillingRepository.save(initial);

        // Update only shipping
        ShippingBillingDTO updateDTO = ShippingBillingDTO.builder()
                .shipping(ShippingBillingDTO.AddressInfo.builder()
                        .fullName("Updated Shipping Only")
                        .address("New Shipping Address")
                        .city("New City")
                        .postalCode("33333")
                        .phone("+3333333333")
                        .build())
                .build();

        // Act
        ShippingBillingDTO result = shippingBillingService.updateShippingBillingInfo("user-partial", updateDTO);

        // Assert - shipping updated, billing should remain null (as per service logic)
        assertThat(result.getShipping().getFullName()).isEqualTo("Updated Shipping Only");
        
        Optional<ShippingBillingInfo> fromDb = shippingBillingRepository.findByUserId("user-partial");
        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getShippingFullName()).isEqualTo("Updated Shipping Only");
        // When billing is not in DTO, it's set to null
        assertThat(fromDb.get().getBillingFullName()).isNull();
    }

    @Test
    @Transactional
    void getShippingBillingInfo_handlesNullFieldsGracefully() {
        // Arrange - create info with some null fields
        ShippingBillingInfo info = ShippingBillingInfo.builder()
                .userId("user-with-nulls")
                .shippingFullName("John Doe")
                .shippingAddress(null)
                .shippingCity("New York")
                .shippingPostalCode(null)
                .shippingPhone("+1234567890")
                .billingFullName(null)
                .billingAddress("Billing Address")
                .billingCity(null)
                .billingPostalCode("12345")
                .billingPhone(null)
                .build();
        shippingBillingRepository.save(info);

        // Act
        ShippingBillingDTO result = shippingBillingService.getShippingBillingInfo("user-with-nulls");

        // Assert
        assertThat(result.getShipping().getFullName()).isEqualTo("John Doe");
        assertThat(result.getShipping().getAddress()).isEmpty();
        assertThat(result.getShipping().getCity()).isEqualTo("New York");
        assertThat(result.getShipping().getPostalCode()).isEmpty();
        
        assertThat(result.getBilling().getFullName()).isEmpty();
        assertThat(result.getBilling().getAddress()).isEqualTo("Billing Address");
        assertThat(result.getBilling().getCity()).isEmpty();
        assertThat(result.getBilling().getPhone()).isEmpty();
    }

    @Test
    @Transactional
    void multipleUsers_canHaveSeparateShippingBillingInfo() {
        // Arrange - create info for multiple users
        shippingBillingRepository.save(ShippingBillingInfo.builder()
                .userId("user-1")
                .shippingFullName("User One")
                .shippingAddress("Address 1")
                .build());

        shippingBillingRepository.save(ShippingBillingInfo.builder()
                .userId("user-2")
                .shippingFullName("User Two")
                .shippingAddress("Address 2")
                .build());

        shippingBillingRepository.save(ShippingBillingInfo.builder()
                .userId("user-3")
                .shippingFullName("User Three")
                .shippingAddress("Address 3")
                .build());

        // Act
        ShippingBillingDTO result1 = shippingBillingService.getShippingBillingInfo("user-1");
        ShippingBillingDTO result2 = shippingBillingService.getShippingBillingInfo("user-2");
        ShippingBillingDTO result3 = shippingBillingService.getShippingBillingInfo("user-3");

        // Assert
        assertThat(result1.getShipping().getFullName()).isEqualTo("User One");
        assertThat(result2.getShipping().getFullName()).isEqualTo("User Two");
        assertThat(result3.getShipping().getFullName()).isEqualTo("User Three");
        
        assertThat(result1.getShipping().getAddress()).isEqualTo("Address 1");
        assertThat(result2.getShipping().getAddress()).isEqualTo("Address 2");
        assertThat(result3.getShipping().getAddress()).isEqualTo("Address 3");
    }
}
