package com.frwss.system.audit.service;


import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.model.AuditLog;
import com.frwss.system.audit.repository.AuditLogRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    // Names of the fields covered by the hash, in the order they are hashed
    private static final String[] FIELD_NAMES = {
            "actionType", "entityType", "entityId",
            "performedBy", "timestamp", "details", "previousHash"
    };

    // How many hex characters of each field's SHA-256 are kept as its fingerprint
    private static final int FINGERPRINT_LENGTH = 16;

    private final AuditLogRepository auditLogRepository;
    private final AuditChainLock auditChainLock;

    public AuditService(AuditLogRepository auditLogRepository, AuditChainLock auditChainLock){
        this.auditLogRepository = auditLogRepository;
        this.auditChainLock = auditChainLock;
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
        // Serialise audit writes: without this, two requests running at the same
        // moment both read the same "latest" entry and chain to it, which shows
        // up later as a false chain break. The lock is held until the surrounding
        // transaction commits, so the next writer always sees this entry.
        auditChainLock.acquire();

        // get the latest log to link the hash chain
        AuditLog latestLog = auditLogRepository.findTopByOrderByIdDesc().orElse(null);

        // If not log exists, this is the first entry (GENESIS)
        String previousHash = latestLog == null
                ? GENESIS_HASH
                : latestLog.getCurrentHash();

        // Truncate to microseconds: PostgreSQL's timestamp column stores
        // microsecond precision, but LocalDateTime.now() can carry nanosecond
        // precision on some JVMs. Without truncating here, the value used to
        // compute the hash and the value that comes back after a save/reload
        // can differ, making every entry fail verification even though
        // nothing was actually tampered with.
        LocalDateTime timestamp = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);

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
        auditLog.setFieldHashes(buildFieldHashes(
                actionType, entityType, entityId, performedBy, timestamp, details, previousHash));

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

        AuditChainResult result = new AuditChainResult();

        if (logs.isEmpty()) {
            return result;
        }

        String expectedPreviousHash = GENESIS_HASH;

        // Pass 1: walk the whole chain and record EVERY break found, instead of
        // stopping at the first one. When a break is found we "resync" using the
        // entry's own stored currentHash so we can keep checking whether anything
        // AFTER this entry is independently consistent, rather than every
        // subsequent entry being falsely reported as broken too.
        for (AuditLog log : logs) {

            // Check 1: does previousHash match what we expect from the chain so far?
            if (!Objects.equals(log.getPreviousHash(), expectedPreviousHash)) {
                result.addBreak(new AuditChainResult.ChainBreak(
                        log.getId(),
                        "Previous hash mismatch. Expected: " + safePrefix(expectedPreviousHash)
                                + "... but got: " + safePrefix(log.getPreviousHash()) + "...",
                        "previousHash"
                ));
            } else {
                // Check 2: does the stored hash match a freshly recalculated hash?
                String recalculatedHash = generateHash(
                        log.getActionType(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getPerformedBy(),
                        log.getTimestamp(),
                        log.getDetails(),
                        log.getPreviousHash()
                );

                if (!Objects.equals(log.getCurrentHash(), recalculatedHash)) {
                    List<String> changedFields = findMismatchedFields(log);

                    String fieldName;
                    String reason;

                    if (changedFields == null) {
                        // Older entry with no field fingerprints: we know it was
                        // altered but cannot say which field.
                        fieldName = "content";
                        reason = "Entry content does not match its hash (the exact field cannot be "
                                + "identified because this entry has no field-level fingerprint)";
                    } else if (changedFields.isEmpty()) {
                        // Every field still matches its fingerprint, so the stored
                        // hash itself is what was changed.
                        fieldName = "currentHash";
                        reason = "Stored hash was altered: all field values still match their fingerprints";
                    } else {
                        fieldName = String.join(", ", changedFields);
                        reason = "Mismatch in field(s): " + fieldName;
                    }

                    result.addBreak(new AuditChainResult.ChainBreak(
                            log.getId(),
                            reason,
                            fieldName
                    ));
                }
            }

            // Whether or not this entry broke, resync forward using whatever hash
            // is physically stored on this row. This never rewrites anything -
            // it only lets verification keep going instead of cascading a single
            // break into a false "everything after this is also broken" report.
            expectedPreviousHash = log.getCurrentHash();
        }

        // Pass 2: any AUDIT_CHAIN_CORRECTED entry (written by acknowledgeChainBreak)
        // references the broken entry's ID in entityId. Use it to mark that break
        // as acknowledged - the original broken row is never touched.
        for (AuditLog log : logs) {
            if (log.getActionType() == AuditActionType.AUDIT_CHAIN_CORRECTED
                    && "AuditChainBreak".equals(log.getEntityType())
                    && log.getEntityId() != null) {

                Long referencedId = parseLongSafely(log.getEntityId());
                if (referencedId == null) continue;

                result.getBreaks().stream()
                        .filter(b -> b.getBrokenAtId().equals(referencedId) && !b.isAcknowledged())
                        .findFirst()
                        .ifPresent(b -> b.acknowledge(log.getPerformedBy(), log.getTimestamp(), log.getDetails()));
            }
        }

        return result;
    }

    /**
     * Records an acknowledgment of a known, investigated chain break.
     * This does NOT modify or recalculate the original broken entry - the
     * evidence of tampering/corruption stays exactly as it was found. It adds
     * a new, normally hash-chained entry that explains the anomaly and lets
     * the chain be reported as valid again from this point forward, the same
     * way a correcting journal entry works in accounting rather than editing
     * a posted ledger line.
     *
     * @param brokenLogId   the id of the AuditLog entry that failed verification
     * @param performedBy   the auditor/admin acknowledging the break
     * @param justification mandatory explanation of what was found and why
     *                      it's being acknowledged (e.g. root cause, ticket ref)
     */
    @Transactional
    public AuditLog acknowledgeChainBreak(Long brokenLogId, String performedBy, String justification) {
        if (brokenLogId == null) {
            throw new IllegalArgumentException("brokenLogId is required");
        }
        if (justification == null || justification.isBlank()) {
            throw new IllegalArgumentException("A justification is required to acknowledge a chain break");
        }

        return log(
                AuditActionType.AUDIT_CHAIN_CORRECTED,
                "AuditChainBreak",
                String.valueOf(brokenLogId),
                performedBy,
                justification.trim()
        );
    }

    /**
     * Returns at most the first 10 characters of a hash, so messages can show a short readable
     * part of it.
     */
    private String safePrefix(String value) {
        if (value == null) return "";
        return value.length() > 10 ? value.substring(0, 10) : value;
    }

    /**
     * Converts text to a Long, returning null instead of throwing when the text is not a number.
     */
    private Long parseLongSafely(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Identifies which field(s) of an entry were changed after it was written.
     *
     * The entry's hash covers all fields at once, so a hash alone cannot say
     * which one changed. Each entry therefore also stores a short fingerprint
     * of every field; here each field is re-fingerprinted and compared with
     * the stored one. The fingerprints are only a diagnostic aid - whether an
     * entry is broken is always decided by the full hash, never by them.
     *
     * @param log The audit log entry whose hash does not match
     * @return the names of the changed fields (empty if every field still
     *         matches, meaning the stored hash itself was altered), or null
     *         if the entry has no usable fingerprints (older entries)
     */
    private List<String> findMismatchedFields(AuditLog log){
        String stored = log.getFieldHashes();
        if (stored == null || stored.isBlank()) {
            return null;
        }

        String[] storedParts = stored.split(",", -1);
        if (storedParts.length != FIELD_NAMES.length) {
            return null;
        }

        String[] currentParts = fingerprintParts(
                log.getActionType(),
                log.getEntityType(),
                log.getEntityId(),
                log.getPerformedBy(),
                log.getTimestamp(),
                log.getDetails(),
                log.getPreviousHash()
        );

        List<String> changed = new ArrayList<>();
        for (int i = 0; i < FIELD_NAMES.length; i++) {
            if (!currentParts[i].equals(storedParts[i])) {
                changed.add(FIELD_NAMES[i]);
            }
        }
        return changed;
    }

    /**
     * Builds the comma-separated per-field fingerprints stored with an entry.
     */
    private String buildFieldHashes(
            AuditActionType actionType,
            String entityType,
            String entityId,
            String performedBy,
            LocalDateTime timestamp,
            String details,
            String previousHash
    ){
        return String.join(",", fingerprintParts(
                actionType, entityType, entityId, performedBy, timestamp, details, previousHash));
    }

    /**
     * Fingerprint (shortened SHA-256) of each hashed field, in FIELD_NAMES order.
     * Values are normalised exactly as in generateHash so both stay consistent.
     */
    private String[] fingerprintParts(
            AuditActionType actionType,
            String entityType,
            String entityId,
            String performedBy,
            LocalDateTime timestamp,
            String details,
            String previousHash
    ){
        String[] values = {
                Objects.toString(actionType, ""),
                Objects.toString(entityType, ""),
                Objects.toString(entityId, ""),
                Objects.toString(performedBy, ""),
                Objects.toString(timestamp, ""),
                Objects.toString(details, ""),
                Objects.toString(previousHash, "")
        };

        String[] parts = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            parts[i] = generateHashFromString(values[i]).substring(0, FINGERPRINT_LENGTH);
        }
        return parts;
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


    /**
     * Converts the action type filter text to an enum value. Blank, ALL or an unknown name mean no
     * filter (null).
     */
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