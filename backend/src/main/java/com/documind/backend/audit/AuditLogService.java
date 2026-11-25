package com.documind.backend.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(UUID workspaceId, UUID actorUserId, String action,
                       String targetType, UUID targetId, Map<String, Object> details) {
        AuditLog entry = new AuditLog(workspaceId, actorUserId, action, targetType, targetId, details);
        auditLogRepository.save(entry);
    }
}
