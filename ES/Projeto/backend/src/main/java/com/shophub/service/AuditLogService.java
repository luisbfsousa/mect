package com.shophub.service;

import com.shophub.dto.AuditLogDTO;
import com.shophub.model.User;
import com.shophub.model.AuditLog;
import com.shophub.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    
    private final AuditLogRepository auditLogRepository;
    private final UserService userService;
    
    @Transactional
    public AuditLog logAdminAction(String adminUserId, String targetUserId, String action, Map<String, Object> details) {
        // Ensure admin exists in users table to satisfy FK constraint
        try {
            if (!userService.userExists(adminUserId)) {
                // Create a minimal admin user record
                String email = adminUserId + "@admin.local";
                String firstName = "Admin";
                String lastName = "User";
                User admin = userService.getOrCreateUser(adminUserId, email, firstName, lastName);
                // Force role to administrator if not already
                admin.setRole("administrator");
            }
        } catch (Exception e) {
            log.warn("Failed to ensure admin user {} exists before audit log: {}", adminUserId, e.getMessage());
        }
        AuditLog auditLog = AuditLog.builder()
                .adminUserId(adminUserId)
                .targetUserId(targetUserId)
                .action(action)
                .details(details != null ? details : new HashMap<>())
                .build();
        
        AuditLog saved = auditLogRepository.save(auditLog);
        
        log.info("Admin action logged: admin={}, target={}, action={}", adminUserId, targetUserId, action);
        
        return saved;
    }
    
    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogsByTargetUser(String targetUserId) {
        return auditLogRepository.findByTargetUserIdOrderByCreatedAtDesc(targetUserId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogsByAdminUser(String adminUserId) {
        return auditLogRepository.findByAdminUserIdOrderByCreatedAtDesc(adminUserId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<AuditLogDTO> getAuditLogsByAction(String action) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private AuditLogDTO convertToDTO(AuditLog auditLog) {
        return AuditLogDTO.builder()
                .id(auditLog.getId())
                .adminUserId(auditLog.getAdminUserId())
                .targetUserId(auditLog.getTargetUserId())
                .action(auditLog.getAction())
                .details(auditLog.getDetails())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
