package com.frwss.system.reconciliation.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AmountAnomalyDetector")
class AmountAnomalyDetectorTest {

    /** Nine copies of `common` plus one `odd` amount: mean and spread are easy to work out. */
    private List<BigDecimal> group(String common, String odd) {
        List<BigDecimal> amounts = new ArrayList<>(Collections.nCopies(9, new BigDecimal(common)));
        amounts.add(new BigDecimal(odd));
        return amounts;
    }

    @Test
    @DisplayName("Flags an amount far above the group average")
    void flagsHighAmount() {
        // nine 100s and one 1000: mean 190, standard deviation 270, so 1000 is 3.0 above
        Optional<String> flag = AmountAnomalyDetector.check(
                new BigDecimal("1000"), "PAYROLL", group("100", "1000"), 2.0, 8);

        assertTrue(flag.isPresent());
        assertEquals(
                "Unusual amount: 1,000.00 is 3.0 standard deviations above the PAYROLL average of 190.00",
                flag.get());
    }

    @Test
    @DisplayName("Flags an amount far below the group average")
    void flagsLowAmount() {
        Optional<String> flag = AmountAnomalyDetector.check(
                new BigDecimal("100"), "SALES", group("1000", "100"), 2.0, 8);

        assertTrue(flag.isPresent());
        assertTrue(flag.get().contains("below"));
    }

    @Test
    @DisplayName("Does not flag a typical amount")
    void typicalAmountNotFlagged() {
        assertTrue(AmountAnomalyDetector.check(
                new BigDecimal("100"), "PAYROLL", group("100", "1000"), 2.0, 8).isEmpty());
    }

    @Test
    @DisplayName("Does not flag when the group is too small to compare against")
    void smallGroupNotFlagged() {
        List<BigDecimal> small = List.of(new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("1000"));

        assertTrue(AmountAnomalyDetector.check(new BigDecimal("1000"), "PAYROLL", small, 2.0, 8).isEmpty());
    }

    @Test
    @DisplayName("Does not flag when every amount in the group is the same")
    void identicalAmountsNotFlagged() {
        List<BigDecimal> same = Collections.nCopies(10, new BigDecimal("500"));

        assertTrue(AmountAnomalyDetector.check(new BigDecimal("500"), "PAYROLL", same, 2.0, 8).isEmpty());
    }

    @Test
    @DisplayName("Does not fail on missing input")
    void missingInputIsEmpty() {
        assertTrue(AmountAnomalyDetector.check(null, "PAYROLL", group("100", "1000"), 2.0, 8).isEmpty());
        assertTrue(AmountAnomalyDetector.check(new BigDecimal("1"), "PAYROLL", null, 2.0, 8).isEmpty());
    }
}
