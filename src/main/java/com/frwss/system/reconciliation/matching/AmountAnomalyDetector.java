package com.frwss.system.reconciliation.matching;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Flags an amount that is unusually far from the other amounts in its group (for example the other
 * PAYROLL records). Pure calculation: it reads no data and writes nothing.
 *
 * The rule is a z-score: how many standard deviations the amount is from the group average. It is
 * flagged when that distance reaches the threshold. A group with too few records, or where every
 * amount is the same, is never flagged because there is nothing reliable to compare against.
 */
public final class AmountAnomalyDetector {

    private AmountAnomalyDetector() {
    }

    /**
     * @param amount       the amount being checked
     * @param groupName    shown in the message, for example "PAYROLL"
     * @param groupAmounts every amount in the group (the checked amount may be one of them)
     * @param threshold    flag when the amount is at least this many standard deviations away
     * @param minGroupSize do not flag when the group has fewer amounts than this
     * @return a one-sentence explanation when the amount is unusual, otherwise empty
     */
    public static Optional<String> check(
            BigDecimal amount,
            String groupName,
            List<BigDecimal> groupAmounts,
            double threshold,
            int minGroupSize
    ) {
        if (amount == null || groupAmounts == null || groupAmounts.size() < minGroupSize) {
            return Optional.empty();
        }

        double mean = groupAmounts.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0.0);
        double variance = groupAmounts.stream()
                .mapToDouble(value -> Math.pow(value.doubleValue() - mean, 2))
                .average()
                .orElse(0.0);
        double standardDeviation = Math.sqrt(variance);

        if (standardDeviation == 0.0) {
            return Optional.empty();
        }

        double distance = (amount.doubleValue() - mean) / standardDeviation;

        if (Math.abs(distance) < threshold) {
            return Optional.empty();
        }

        return Optional.of(String.format(
                Locale.US,
                "Unusual amount: %,.2f is %.1f standard deviations %s the %s average of %,.2f",
                amount,
                Math.abs(distance),
                distance < 0 ? "below" : "above",
                groupName,
                mean
        ));
    }
}
