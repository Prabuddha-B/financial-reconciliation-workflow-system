package com.frwss.system.audit.service;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.model.AuditLog;
import com.frwss.system.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Audit Service Unit Tests")
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        // Mockito injects mocked dependencies.
    }

    // =========================================================================
    // log()
    // =========================================================================

    @Test
    @DisplayName("Should use GENESIS as the previous hash for the very first entry")
    void log_noPriorEntries_usesGenesisAsPreviousHash() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(auditLogRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        AuditLog saved = auditService.log(
                AuditActionType.RECONCILIATION_STARTED,
                "ReconciliationRun",
                "RUN-1",
                "admin",
                "Reconciliation started"
        );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("GENESIS", saved.getPreviousHash());
        assertNotNull(saved.getCurrentHash());
        assertEquals(64, saved.getCurrentHash().length(), "SHA-256 hex digest should be 64 characters");
    }

    @Test
    @DisplayName("Should chain the new entry's previousHash to the prior entry's currentHash")
    void log_priorEntryExists_chainsToPriorCurrentHash() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        AuditLog priorEntry = new AuditLog();
        priorEntry.setId(1L);
        priorEntry.setCurrentHash("abc123previoushash");

        when(auditLogRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(priorEntry));
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        AuditLog saved = auditService.log(
                AuditActionType.EXACT_MATCH_FOUND,
                "ReconciliationResult",
                "ACC-001",
                "admin",
                "Exact match found"
        );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals("abc123previoushash", saved.getPreviousHash());
        assertNotEquals(saved.getPreviousHash(), saved.getCurrentHash());
    }

    @Test
    @DisplayName("Should produce different hashes for entries with different content")
    void log_differentContent_producesDifferentHashes() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        List<AuditLog> savedLogs = new ArrayList<>();
        stubChainedPersistence(savedLogs);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        auditService.log(AuditActionType.EXACT_MATCH_FOUND, "ReconciliationResult", "ACC-001", "admin", "First match");
        auditService.log(AuditActionType.EXACT_MATCH_FOUND, "ReconciliationResult", "ACC-002", "admin", "Second match");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertNotEquals(savedLogs.get(0).getCurrentHash(), savedLogs.get(1).getCurrentHash());
    }

    // =========================================================================
    // verifyAuditChainDetailed()
    // =========================================================================

    @Test
    @DisplayName("Should report a valid chain when there are no entries at all")
    void verifyAuditChainDetailed_noEntries_reportsValid() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(auditLogRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        AuditChainResult result = auditService.verifyAuditChainDetailed();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertTrue(result.isValid());
        assertFalse(result.hasAnyBreaks());
    }

    @Test
    @DisplayName("Should report a valid chain when every entry is genuinely intact")
    void verifyAuditChainDetailed_intactChain_reportsValid() {

        // ---------------------------------------------------------------------
        // Arrange: build a real 3-entry chain via the service's own log() method
        // ---------------------------------------------------------------------

        List<AuditLog> savedLogs = new ArrayList<>();
        stubChainedPersistence(savedLogs);

        auditService.log(AuditActionType.RECONCILIATION_STARTED, "ReconciliationRun", "RUN-1", "admin", "started");
        auditService.log(AuditActionType.EXACT_MATCH_FOUND, "ReconciliationResult", "ACC-001", "admin", "matched");
        auditService.log(AuditActionType.RECONCILIATION_COMPLETED, "ReconciliationRun", "RUN-1", "admin", "completed");

        when(auditLogRepository.findAllByOrderByIdAsc()).thenReturn(savedLogs);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        AuditChainResult result = auditService.verifyAuditChainDetailed();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertTrue(result.isValid());
        assertFalse(result.hasAnyBreaks());
    }

    @Test
    @DisplayName("Should detect a break when an entry's previousHash no longer matches the prior entry's currentHash")
    void verifyAuditChainDetailed_previousHashTampered_reportsBreak() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        List<AuditLog> savedLogs = new ArrayList<>();
        stubChainedPersistence(savedLogs);

        auditService.log(AuditActionType.RECONCILIATION_STARTED, "ReconciliationRun", "RUN-1", "admin", "started");
        auditService.log(AuditActionType.EXACT_MATCH_FOUND, "ReconciliationResult", "ACC-001", "admin", "matched");

        // Simulate tampering: the second entry's previousHash no longer matches
        // the first entry's currentHash.
        savedLogs.get(1).setPreviousHash("SOMETHING-ELSE-ENTIRELY");

        when(auditLogRepository.findAllByOrderByIdAsc()).thenReturn(savedLogs);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        AuditChainResult result = auditService.verifyAuditChainDetailed();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertFalse(result.isValid());
        assertTrue(result.hasAnyBreaks());
        assertEquals(1, result.getTotalBreakCount());

        AuditChainResult.ChainBreak chainBreak = result.getBreaks().get(0);
        assertEquals(savedLogs.get(1).getId(), chainBreak.getBrokenAtId());
        assertEquals("previousHash", chainBreak.getFieldName());
    }

    @Test
    @DisplayName("Should still detect content tampering as a break, even though the reported field name is unreliable")
    void verifyAuditChainDetailed_contentTampered_reportsBreak() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        List<AuditLog> savedLogs = new ArrayList<>();
        stubChainedPersistence(savedLogs);

        auditService.log(AuditActionType.EXACT_MATCH_FOUND, "ReconciliationResult", "ACC-001", "admin", "original details");

        // Simulate tampering: the details field changes after the hash was
        // computed and stored, so currentHash no longer matches the content.
        savedLogs.get(0).setDetails("TAMPERED details");

        when(auditLogRepository.findAllByOrderByIdAsc()).thenReturn(savedLogs);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        AuditChainResult result = auditService.verifyAuditChainDetailed();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertFalse(result.isValid());
        assertEquals(1, result.getTotalBreakCount());

        // NOTE / KNOWN ISSUE: AuditService#findMismatchField compares an
        // incremental partial-string hash against the full 7-field stored
        // hash. Those can only coincidentally match, so in practice this
        // method always reports the FIRST field ("actionType") regardless
        // of which field actually changed - confirmed by tampering
        // "details" here and getting "actionType" back. The *detection*
        // that the entry was tampered with is still correct and reliable;
        // only this diagnostic field name is misleading. This test pins
        // down the current behavior so a future fix to findMismatchField
        // is a deliberate, visible change rather than a silent one.
        assertEquals("actionType", result.getBreaks().get(0).getFieldName());
    }

    // =========================================================================
    // acknowledgeChainBreak()
    // =========================================================================

    @Test
    @DisplayName("Should reject a null broken log id")
    void acknowledgeChainBreak_nullId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                auditService.acknowledgeChainBreak(null, "auditor", "Investigated and confirmed benign"));
    }

    @Test
    @DisplayName("Should reject a blank justification")
    void acknowledgeChainBreak_blankJustification_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                auditService.acknowledgeChainBreak(5L, "auditor", "   "));
    }

    @Test
    @DisplayName("Should log a correction entry referencing the broken entry, with a trimmed justification")
    void acknowledgeChainBreak_validInput_logsCorrectionEntry() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(auditLogRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        AuditLog result = auditService.acknowledgeChainBreak(
                42L, "auditor", "  Investigated - false positive due to clock skew.  ");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(AuditActionType.AUDIT_CHAIN_CORRECTED, result.getActionType());
        assertEquals("AuditChainBreak", result.getEntityType());
        assertEquals("42", result.getEntityId());
        assertEquals("auditor", result.getPerformedBy());
        assertEquals("Investigated - false positive due to clock skew.", result.getDetails());
    }

    // =========================================================================
    // getFilteredLogs()
    // =========================================================================

    @Test
    @DisplayName("Should filter logs by exact action type")
    void getFilteredLogs_byActionType_returnsOnlyMatchingEntries() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        AuditLog matching = logEntry(AuditActionType.EXACT_MATCH_FOUND, "system", "user1");
        AuditLog nonMatching = logEntry(AuditActionType.ORPHAN_RECORD_FOUND, "system", "user1");

        when(auditLogRepository.findAllByOrderByTimestampDesc())
                .thenReturn(List.of(matching, nonMatching));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<AuditLog> result = auditService.getFilteredLogs("EXACT_MATCH_FOUND", null, null, null);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, result.size());
        assertEquals(matching, result.get(0));
    }

    @Test
    @DisplayName("Should filter logs by performedBy as a case-insensitive substring match")
    void getFilteredLogs_byPerformedBySubstring_isCaseInsensitive() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        AuditLog matching = logEntry(AuditActionType.EXACT_MATCH_FOUND, "system", "Navindu.Rajamanthri");
        AuditLog nonMatching = logEntry(AuditActionType.EXACT_MATCH_FOUND, "system", "someone-else");

        when(auditLogRepository.findAllByOrderByTimestampDesc())
                .thenReturn(List.of(matching, nonMatching));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<AuditLog> result = auditService.getFilteredLogs(null, null, "navindu", null);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(1, result.size());
        assertEquals(matching, result.get(0));
    }

    @Test
    @DisplayName("Should return every log, unlimited, when limit is ALL")
    void getFilteredLogs_limitAll_returnsEveryEntry() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        List<AuditLog> allLogs = new ArrayList<>();
        for (int i = 0; i < 75; i++) {
            allLogs.add(logEntry(AuditActionType.EXACT_MATCH_FOUND, "system", "admin"));
        }

        when(auditLogRepository.findAllByOrderByTimestampDesc()).thenReturn(allLogs);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        List<AuditLog> result = auditService.getFilteredLogs(null, null, null, "ALL");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(75, result.size());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Stubs the repository so that auditService.log() behaves like a real,
     * in-memory audit table: each save() is appended to savedLogs and
     * assigned an incrementing id, and each subsequent log() call correctly
     * sees the previously "saved" entry as the latest one. This lets tests
     * build a genuinely hash-chained sequence of entries using the real
     * log() method, instead of hand-constructing fake hashes.
     */
    private void stubChainedPersistence(List<AuditLog> savedLogs) {
        long[] nextId = {1L};

        when(auditLogRepository.findTopByOrderByIdDesc())
                .thenAnswer(invocation -> savedLogs.isEmpty()
                        ? Optional.empty()
                        : Optional.of(savedLogs.get(savedLogs.size() - 1)));

        when(auditLogRepository.save(any(AuditLog.class)))
                .thenAnswer(invocation -> {
                    AuditLog entry = invocation.getArgument(0);
                    entry.setId(nextId[0]++);
                    savedLogs.add(entry);
                    return entry;
                });
    }

    private AuditLog logEntry(AuditActionType actionType, String entityType, String performedBy) {
        AuditLog log = new AuditLog();
        log.setActionType(actionType);
        log.setEntityType(entityType);
        log.setPerformedBy(performedBy);
        return log;
    }
}