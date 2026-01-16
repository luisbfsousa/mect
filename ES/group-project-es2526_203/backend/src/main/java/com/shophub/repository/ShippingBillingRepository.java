package com.shophub.repository;

import com.shophub.model.ShippingBillingInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ShippingBillingRepository extends JpaRepository<ShippingBillingInfo, Long> {
    Optional<ShippingBillingInfo> findByUserId(String userId);
    boolean existsByUserId(String userId);
    
    @Transactional
    @Modifying
    void deleteByUserId(String userId);
}