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

/**
 * Service for managing the SHA-256 hash-chained audit trail.
 * Each audit entry is cryptographically linked to the previous one,
 * ensuring tamper-evident logging.
 */

@Service
public class AuditService {
    private static final String GENESIS_HASH = "GENESIS";

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository){
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Creates a new audit log entry with hash chaining.
     *
     * @param actionType The type of action being logged
     * @param entityType The type of entity affected
     * @param entityId The ID of the affected entity
     * @param performedBy The user who performed the action
     * @param details Additional details about the action
     * @return The saved AuditLog entity
     */
    @Transactional
    public AuditLog log(
            AuditActionType actionType,
            String entityType,
            String entityId,
            String performedBy,
            String details
    ) {
        // get the latest log to link the hash chain
        AuditLog latestLog = auditLogRepository.findTopByOrderByIdDesc().orElse(null);

        // If not log exists, this is the first entry (GENESIS)
        String previousHash = latestLog == null
                ? GENESIS_HASH
                : latestLog.getCurrentHash();

        LocalDateTime timestamp = LocalDateTime.now();

        // generate the hash for this entry based on its current content and previous hash
        String currentHash = generateHash(
                actionType,
                entityType,
                entityId,
                performedBy,
                timestamp,
                details,
                previousHash
        );

        // Build the audit log entry
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

    /**
     * Verifies the integrity of the entire audit chain.
     * Checks that each entry's previousHash matches the previous entry's currentHash,
     * and that each entry's currentHash matches a recalculated hash.
     *
     * @return AuditChainResult indicating whether the chain is valid
     */

    @Transactional(readOnly = true)
    public AuditChainResult verifyAuditChainDetailed(){
        List<AuditLog> logs = auditLogRepository.findAllByOrderByIdAsc();

        if (logs.isEmpty()) {
            return new AuditChainResult(true, null, "No logs to verify", null);
        }

        String expectedPreviousHash = GENESIS_HASH;

        for(AuditLog log : logs){
            // check 1 : Does previousHash match what we expect from the chain?
            if(!Objects.equals(log.getPreviousHash(), expectedPreviousHash)){
                return new AuditChainResult(false,
                        log.getId(),
                        "Previous hash mismatch. Expected: " + expectedPreviousHash.substring(0, 10) + "... but got: " + log.getPreviousHash().substring(0, 10) + "...",
                        "previousHash");
            }

            // Check 2 : Does the stored hash match a freshly calculated hash?
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
                // Find which specific field caused the mismatch
                String mismatchField = findMismatchField(log);
                return new AuditChainResult(false,
                        log.getId(),
                        "Mismatch in field: " + mismatchField,
                        mismatchField);
            }

            // Move the chain forward
            expectedPreviousHash = log.getCurrentHash();
        }
        return new AuditChainResult(true, null, "Audit chain is valid", null);

    }

    /**
     * Identifies which field caused a hash mismatch.
     * Builds the hash incrementally to pinpoint the exact field.
     *
     * @param log The audit log entry with the mismatch
     * @return The name of the field that caused the mismatch
     */
    private String findMismatchField(AuditLog log){
        // Define field values and their names in order
        Object[] fieldValues = {
                log.getActionType() != null ? log.getActionType().name() : "",
                log.getEntityType() != null ? log.getEntityType() : "",
                log.getEntityId() != null ? log.getEntityId() : "",
                log.getPerformedBy() != null ? log.getPerformedBy() : "",
                log.getTimestamp() != null ? log.getTimestamp().toString() : "",
                log.getDetails() != null ? log.getDetails() : "",
                log.getPreviousHash() != null ? log.getPreviousHash() : ""
        };

        String[] fieldNames = {
                "actionType", "entityType", "entityId",
                "performedBy", "timestamp", "details", "previousHash"
        };

        // Build up the concatenated string incrementally to find where it breaks
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < fieldValues.length; i++){
            if(i > 0) sb.append("|");
            sb.append(fieldValues[i]);

            // Generate has of the current state
            String partialHash = generateHashFromString(sb.toString());

            // If the partial hash doesn't match the stored hash, this field is causing the issue

            if(!partialHash.equals(log.getCurrentHash())){
                return fieldNames[i];
            }

        }
        return "unknown";
    }

    /**
     * Generates a SHA-256 hash from a raw string.
     *
     * @param rawData The raw string to hash
     * @return The hexadecimal SHA-256 hash
     */

    private String generateHashFromString(String rawData){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawData.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(encodedHash);
        } catch(Exception e){
            throw new RuntimeException("Failed to generate hash", e);
        }
    }

    /**
     * Generates a SHA-256 hash from all audit fields.
     *
     * @param actionType The action type
     * @param entityType The entity type
     * @param entityId The entity ID
     * @param performedBy The user who performed the action
     * @param timestamp The timestamp
     * @param details Additional details
     * @param previousHash The previous entry's hash
     * @return The hexadecimal SHA-256 hash
     */

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
        } catch (Exception e){
            throw new RuntimeException("Failed to generate audit hash", e);
        }
    }


    /**
     * Gets all audit logs in descending order (newest first).
     *
     * @return List of all audit logs
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getAllLogs(){
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    /**
     * Gets filtered audit logs with pagination support.
     *
     * @param actionType Filter by action type (optional)
     * @param entityType Filter by entity type (optional)
     * @param performedBy Filter by user (optional)
     * @param limit Maximum number of results (optional)
     * @return Filtered list of audit logs
     */
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

    /**
     * Parses the limit parameter.
     *
     * @param limit The limit string ("ALL" or a number)
     * @return The parsed limit, or -1 for ALL
     */
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

    /**
     * Cleans a filter value by trimming whitespace.
     *
     * @param value The filter value to clean
     * @return The cleaned value, or null if blank
     */
    private String cleanFilterValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * Gets the total number of audit log entries.
     *
     * @return Total count of audit logs
     */
    @Transactional(readOnly = true)
    public long getTotalLogCount(){
        return auditLogRepository.count();
    }

}
