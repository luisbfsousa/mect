package com.shophub.service;

import com.shophub.dto.ShippingBillingDTO;
import com.shophub.model.ShippingBillingInfo;
import com.shophub.repository.ShippingBillingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingBillingService {
    
    private final ShippingBillingRepository shippingBillingRepository;
    
    @Transactional(readOnly = true)
    public ShippingBillingDTO getShippingBillingInfo(String userId) {
        log.info("Fetching shipping/billing info for user: {}", userId);
        
        return shippingBillingRepository.findByUserId(userId)
                .map(this::convertToDTO)
                .orElseGet(() -> {
                    log.info("No shipping/billing info found for user: {}, returning empty", userId);
                    return createEmptyDTO();
                });
    }
    
    @Transactional
    public ShippingBillingDTO updateShippingBillingInfo(String userId, ShippingBillingDTO dto) {
        log.info("Updating shipping/billing info for user: {}", userId);
        
        ShippingBillingInfo info = shippingBillingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating new shipping/billing info for user: {}", userId);
                    return ShippingBillingInfo.builder()
                            .userId(userId)
                            .build();
                });
        
        // Update shipping information
        if (dto.getShipping() != null) {
            info.setShippingFullName(dto.getShipping().getFullName());
            info.setShippingAddress(dto.getShipping().getAddress());
            info.setShippingCity(dto.getShipping().getCity());
            info.setShippingPostalCode(dto.getShipping().getPostalCode());
            info.setShippingPhone(dto.getShipping().getPhone());
        } else {
            // If shipping info is not provided in DTO, clear existing shipping fields
            info.setShippingFullName(null);
            info.setShippingAddress(null);
            info.setShippingCity(null);
            info.setShippingPostalCode(null);
            info.setShippingPhone(null);
        }
        
        // Update billing information
        if (dto.getBilling() != null) {
            info.setBillingFullName(dto.getBilling().getFullName());
            info.setBillingAddress(dto.getBilling().getAddress());
            info.setBillingCity(dto.getBilling().getCity());
            info.setBillingPostalCode(dto.getBilling().getPostalCode());
            info.setBillingPhone(dto.getBilling().getPhone());
        } else {
            // If billing info is not provided in DTO, clear existing billing fields
            info.setBillingFullName(null);
            info.setBillingAddress(null);
            info.setBillingCity(null);
            info.setBillingPostalCode(null);
            info.setBillingPhone(null);
        }
        
        ShippingBillingInfo saved = shippingBillingRepository.save(info);
        log.info("Successfully updated shipping/billing info for user: {}", userId);
        
        return convertToDTO(saved);
    }
    
    @Transactional
    public void deleteShippingBillingInfo(String userId) {
        log.info("Deleting shipping/billing info for user: {}", userId);
        shippingBillingRepository.deleteByUserId(userId);
    }
    
    private ShippingBillingDTO convertToDTO(ShippingBillingInfo info) {
        return ShippingBillingDTO.builder()
                .shipping(ShippingBillingDTO.AddressInfo.builder()
                        .fullName(info.getShippingFullName() != null ? info.getShippingFullName() : "")
                        .address(info.getShippingAddress() != null ? info.getShippingAddress() : "")
                        .city(info.getShippingCity() != null ? info.getShippingCity() : "")
                        .postalCode(info.getShippingPostalCode() != null ? info.getShippingPostalCode() : "")
                        .phone(info.getShippingPhone() != null ? info.getShippingPhone() : "")
                        .build())
                .billing(ShippingBillingDTO.AddressInfo.builder()
                        .fullName(info.getBillingFullName() != null ? info.getBillingFullName() : "")
                        .address(info.getBillingAddress() != null ? info.getBillingAddress() : "")
                        .city(info.getBillingCity() != null ? info.getBillingCity() : "")
                        .postalCode(info.getBillingPostalCode() != null ? info.getBillingPostalCode() : "")
                        .phone(info.getBillingPhone() != null ? info.getBillingPhone() : "")
                        .build())
                .build();
    }
    
    private ShippingBillingDTO createEmptyDTO() {
        return ShippingBillingDTO.builder()
                .shipping(ShippingBillingDTO.AddressInfo.builder()
                        .fullName("")
                        .address("")
                        .city("")
                        .postalCode("")
                        .phone("")
                        .build())
                .billing(ShippingBillingDTO.AddressInfo.builder()
                        .fullName("")
                        .address("")
                        .city("")
                        .postalCode("")
                        .phone("")
                        .build())
                .build();
    }
}