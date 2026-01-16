package com.shophub.repository;

import com.shophub.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findByTargetUserIdOrderByCreatedAtDesc(String targetUserId);
    
    List<AuditLog> findByAdminUserIdOrderByCreatedAtDesc(String adminUserId);
    
    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);
}
