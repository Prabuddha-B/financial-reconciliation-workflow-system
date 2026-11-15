package com.frwss.system.reconciliation.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReferenceSimilarity")
class ReferenceSimilarityTest {

    @Test
    @DisplayName("Identical references score 100")
    void identical_is100() {
        assertEquals(100.0, ReferenceSimilarity.percent("RC-2026-001", "RC-2026-001"));
    }

    @Test
    @DisplayName("Case and punctuation are ignored")
    void caseAndPunctuationIgnored() {
        assertEquals(100.0, ReferenceSimilarity.percent("rc-2026-001", "RC2026001"));
    }

    @Test
    @DisplayName("One changed character out of nine scores 88.9")
    void oneCharacterDifferent() {
        assertEquals(88.9, ReferenceSimilarity.percent("RC-2026-001", "RC-2026-002"));
    }

    @Test
    @DisplayName("A missing character lowers the score")
    void oneCharacterMissing() {
        // RC2026001 vs RC202601: distance 1 over the longer length 9
        assertEquals(88.9, ReferenceSimilarity.percent("RC-2026-001", "RC-202601"));
    }

    @Test
    @DisplayName("Unrelated references score low")
    void unrelatedIsLow() {
        assertTrue(ReferenceSimilarity.percent("RC-2026-001", "INV-77-ABC") < 30.0);
    }

    @Test
    @DisplayName("Missing or empty references score 0")
    void missingIsZero() {
        assertEquals(0.0, ReferenceSimilarity.percent(null, "RC-2026-001"));
        assertEquals(0.0, ReferenceSimilarity.percent("RC-2026-001", null));
        assertEquals(0.0, ReferenceSimilarity.percent("", "RC-2026-001"));
        assertEquals(0.0, ReferenceSimilarity.percent("---", "RC-2026-001"));
    }

    @Test
    @DisplayName("The score does not depend on the order of the two references")
    void symmetric() {
        assertEquals(
                ReferenceSimilarity.percent("PO-2026-A01", "PO-2026-B07"),
                ReferenceSimilarity.percent("PO-2026-B07", "PO-2026-A01"));
    }
}
