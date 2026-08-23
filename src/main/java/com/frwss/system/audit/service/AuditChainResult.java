package com.frwss.system.audit.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Result of a full audit-chain verification pass.
 * A chain can contain multiple breaks; each break can independently be
 * "acknowledged" via a correction entry without altering the original
 * (still-broken) historical row. The chain is considered valid overall
 * only if it has no unresolved breaks.
 */
public class AuditChainResult {

    /**
     * A single point where the chain failed verification.
     */
    public static class ChainBreak {
        private final Long brokenAtId;
        private final String reason;
        private final String fieldName;

        private boolean acknowledged = false;
        private String acknowledgedBy;
        private LocalDateTime acknowledgedAt;
        private String acknowledgmentJustification;

        public ChainBreak(Long brokenAtId, String reason, String fieldName) {
            this.brokenAtId = brokenAtId;
            this.reason = reason;
            this.fieldName = fieldName;
        }

        public Long getBrokenAtId() { return brokenAtId; }
        public String getReason() { return reason; }
        public String getFieldName() { return fieldName; }

        public boolean isAcknowledged() { return acknowledged; }
        public String getAcknowledgedBy() { return acknowledgedBy; }
        public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
        public String getAcknowledgmentJustification() { return acknowledgmentJustification; }

        public void acknowledge(String performedBy, LocalDateTime when, String justification) {
            this.acknowledged = true;
            this.acknowledgedBy = performedBy;
            this.acknowledgedAt = when;
            this.acknowledgmentJustification = justification;
        }
    }

    private final List<ChainBreak> breaks = new ArrayList<>();

    /** True only when there are zero breaks, or every break has been acknowledged. */
    public boolean isValid() {
        return breaks.stream().noneMatch(b -> !b.isAcknowledged());
    }

    public boolean hasAnyBreaks() {
        return !breaks.isEmpty();
    }

    public List<ChainBreak> getBreaks() {
        return breaks;
    }

    public List<ChainBreak> getUnresolvedBreaks() {
        return breaks.stream().filter(b -> !b.isAcknowledged()).toList();
    }

    public int getUnresolvedCount() {
        return getUnresolvedBreaks().size();
    }

    public int getTotalBreakCount() {
        return breaks.size();
    }

    void addBreak(ChainBreak chainBreak) {
        breaks.add(chainBreak);
    }

    /**
     * Convenience accessor for templates/legacy callers: the first
     * unresolved break, or null if the chain is fully valid.
     */
    public ChainBreak getFirstUnresolvedBreak() {
        return getUnresolvedBreaks().stream().findFirst().orElse(null);
    }
}