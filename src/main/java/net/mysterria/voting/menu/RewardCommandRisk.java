package net.mysterria.voting.menu;

import java.util.Locale;
import java.util.Set;

/** Classifies console reward command templates that hand out items, currency or permissions. */
final class RewardCommandRisk {
    private static final Set<String> HIGH_RISK = Set.of(
            "give", "item", "i", "eco", "economy", "money", "pay", "xp", "experience", "lp", "luckperms");

    private RewardCommandRisk() {
    }

    static boolean isHighRisk(String template) {
        if (template == null) return false;
        for (String token : template.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            String word = token.startsWith("/") ? token.substring(1) : token;
            int namespace = word.indexOf(':');
            if (namespace >= 0) word = word.substring(namespace + 1);
            if (HIGH_RISK.contains(word)) return true;
        }
        return false;
    }
}
