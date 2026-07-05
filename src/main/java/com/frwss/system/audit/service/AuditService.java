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
    public AuditChainResult verifyAuditChainDetailed(){
        List<AuditLog> logs = auditLogRepository.findAllByOrderByIdAsc();
        String expectedPreviousHash = GENESIS_HASH;

        for(AuditLog log : logs){
            if(!Objects.equals(log.getPreviousHash(), expectedPreviousHash)){
                return new AuditChainResult(false, log.getId(), "Previous hash mismatch", "previousHash");
            }

            // Recalculate full hash
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
                // Localize mismatch by checking each field
                String mismatchField = findMismatchField(log, log.getCurrentHash());
                return new AuditChainResult(false, log.getId(),
                        "Mismatch in field: " + mismatchField, mismatchField);
            }

            expectedPreviousHash = log.getCurrentHash();
        }
        return new AuditChainResult(true, null, null, null);

    }

    private String findMismatchField(AuditLog log, String storedHash){
        String[] fieldValues = {
                Objects.toString(log.getActionType(), ""),
                Objects.toString(log.getEntityType(), ""),
                Objects.toString(log.getEntityId(), ""),
                Objects.toString(log.getPerformedBy(), ""),
                Objects.toString(log.getTimestamp(), ""),
                Objects.toString(log.getDetails(), ""),
                Objects.toString(log.getPreviousHash(),"")
        };

        String[] fieldNames = {
                "actionType", "entityType", "entityId",
                "performedBy", "timestamp", "details", "previousHash"
        };

        // Build up the concatenated string incrementally
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < fieldValues.length; i++){
            if(i > 0) sb.append("|");
            sb.append(fieldValues[i]);

            String partialHash = generateHash(
                    log.getActionType(),
                    log.getEntityType(),
                    log.getEntityId(),
                    log.getPerformedBy(),
                    log.getTimestamp(),
                    log.getDetails(),
                    log.getPreviousHash()
            );

            if(!partialHash.equals(storedHash) && i == fieldValues.length -1){
                return fieldNames[i];
            }

        }
        return "unknown";
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

    @Transactional(readOnly = true)
    public List<AuditLog> getFilteredLogs(
            String actionType,
            String entityType,
            String performedBy,
            String limit
    ) {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByTimestampDesc();

        String cleanedActionType = cleanFilterValue(actionType);
        String cleanedEntityType = cleanFilterValue(entityType);
        String cleanedPerformedBy = cleanFilterValue(performedBy);

        List<AuditLog> filteredLogs = logs.stream()
                .filter(log -> cleanedActionType == null
                        || cleanedActionType.equals("ALL")
                        || log.getActionType().name().equals(cleanedActionType))

                .filter(log -> cleanedEntityType == null
                        || (log.getEntityType() != null
                        && log.getEntityType().toLowerCase().contains(cleanedEntityType.toLowerCase())))

                .filter(log -> cleanedPerformedBy == null
                        || (log.getPerformedBy() != null
                        && log.getPerformedBy().toLowerCase().contains(cleanedPerformedBy.toLowerCase())))

                .toList();

        int selectedLimit = parseLimit(limit);

        if (selectedLimit == -1) {
            return filteredLogs;
        }

        return filteredLogs.stream()
                .limit(selectedLimit)
                .toList();
    }


    private AuditActionType parseActionType(String actionType) {
        if (actionType == null || actionType.isBlank() || actionType.equals("ALL")) {
            return null;
        }

        try {
            return AuditActionType.valueOf(actionType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private int parseLimit(String limit){
        if(limit == null || limit.isBlank()){
            return 50;
        }
        if(limit.equalsIgnoreCase("ALL")){
            return -1;
        }

        try{
            return Integer.parseInt(limit);
        } catch (NumberFormatException e){
            return 50;
        }
    }

    private String cleanFilterValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    @Transactional(readOnly = true)
    public long getTotalLogCount(){
        return auditLogRepository.count();
    }

}
