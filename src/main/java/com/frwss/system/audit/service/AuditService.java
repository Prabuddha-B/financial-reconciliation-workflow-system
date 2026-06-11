package com.frwss.system.audit.service;


import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.model.AuditLog;
import com.frwss.system.audit.repository.AuditLogRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

@Service
public class AuditService {
    private static final String GENESIS_HASH = "GENESIS";

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository){
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public AuditLog log(
            AuditActionType actionType,
            String entityType,
            String entityId,
            String performedBy,
            String details
    ) {
        AuditLog latestLog = auditLogRepository.findTopByOrderByIdDesc().orElse(null);

        String previousHash = latestLog == null
                ? GENESIS_HASH
                : latestLog.getCurrentHash();

        LocalDateTime timestamp = LocalDateTime.now();

        String currentHash = generateHash(
                actionType,
                entityType,
                entityId,
                performedBy,
                timestamp,
                details,
                previousHash
        );

        AuditLog auditLog = new AuditLog();
        auditLog.setActionType(actionType);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setPerformedBy(performedBy);
        auditLog.setTimestamp(timestamp);
        auditLog.setDetails(details);
        auditLog.setPreviousHash(previousHash);
        auditLog.setCurrentHash(currentHash);

        return auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public boolean verifyAuditChain(){
        List<AuditLog> logs = auditLogRepository.findAllByOrderByIdAsc();
        String expectedPreviousHash = GENESIS_HASH;

        for(AuditLog log : logs){
            if(!Objects.equals(log.getPreviousHash(), expectedPreviousHash)){
                return false;
            }

            String recalculatedHash = generateHash(
                    log.getActionType(),
                    log.getEntityType(),
                    log.getEntityId(),
                    log.getPerformedBy(),
                    log.getTimestamp(),
                    log.getDetails(),
                    log.getPreviousHash()
            );

            if (!Objects.equals(log.getCurrentHash(), recalculatedHash)){
                return false;
            }

            expectedPreviousHash = log.getCurrentHash();
        }
        return true;

    }

    private String generateHash(
            AuditActionType actionType,
            String entityType,
            String entityId,
            String performedBy,
            LocalDateTime timestamp,
            String details,
            String previousHash
    ){
        try{
            String rawData = String.join("|",
                    Objects.toString(actionType, ""),
                    Objects.toString(entityType, ""),
                    Objects.toString(entityId, ""),
                    Objects.toString(performedBy, ""),
                    Objects.toString(timestamp, ""),
                    Objects.toString(details, ""),
                    Objects.toString(previousHash, "")
            );

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(encodedHash);
        } catch(Exception e){
            throw new RuntimeException("Failed to generate audit hash", e);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getAllLogs(){
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

}
