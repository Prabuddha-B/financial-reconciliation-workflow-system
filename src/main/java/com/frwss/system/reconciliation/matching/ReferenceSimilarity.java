package com.frwss.system.reconciliation.matching;

/**
 * Compares two reference numbers and says how alike they are, as a percentage. Pure calculation:
 * it reads no data and writes nothing.
 *
 * Both references are upper-cased and stripped of everything except letters and digits first, so
 * "rc-2026-001" and "RC2026001" count as identical. The score is then
 * (1 - edit distance / length of the longer reference) * 100, where the edit distance (Levenshtein)
 * is the number of single-character inserts, deletes or changes needed to turn one into the other.
 */
public final class ReferenceSimilarity {

    private ReferenceSimilarity() {
    }

    /**
     * @return 0 to 100, rounded to one decimal place. 0 when either reference is missing or has no
     *         letters or digits; 100 when they are the same after cleaning.
     */
    public static double percent(String first, String second) {
        String a = clean(first);
        String b = clean(second);

        if (a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        if (a.equals(b)) {
            return 100.0;
        }

        int distance = editDistance(a, b);
        int longest = Math.max(a.length(), b.length());
        double similarity = (1.0 - (double) distance / longest) * 100.0;

        return Math.round(similarity * 10.0) / 10.0;
    }

    private static String clean(String reference) {
        if (reference == null) {
            return "";
        }
        return reference.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private static int editDistance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int change = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + change
                );
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[b.length()];
    }
}
