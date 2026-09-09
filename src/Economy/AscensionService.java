package Economy;

import Entities.Character;
import Entities.Items.AscensionCrystal;

/**
 * AscensionService — handles character ascension.
 *
 * Ascension unlocks the next 10-level bracket for a character.
 * Each phase requires a specific quantity of that character's crystal:
 *   Phase 1 → 4     (unlocks lv 11–20)
 *   Phase 2 → 8     (unlocks lv 21–30)
 *   Phase 3 → 16    (unlocks lv 31–40)
 *   Phase 4 → 32    (unlocks lv 41–50)
 *   Phase 5 → 64    (unlocks lv 51–60)
 *
 * A character MUST be at their current phase level cap before ascending
 * (e.g. must be Lv10 to unlock Phase 1, Lv20 for Phase 2, etc.).
 *
 * After a successful ascension, applyAscensionBoost(phase) is called on
 * the character so subclasses can apply their specific stat increase.
 */
public class AscensionService {

    public enum AscensionResult {
        SUCCESS,
        /** Character has not yet reached the current phase level cap. */
        NOT_AT_LEVEL_CAP,
        /** Character is already fully ascended (Phase 5). */
        ALREADY_MAX_PHASE,
        /** No crystals for this character exist in the inventory. */
        NO_CRYSTALS,
        /** Not enough crystals for the next phase. */
        INSUFFICIENT_CRYSTALS
    }

    /**
     * Attempts to ascend a character to the next phase.
     *
     * @param character the character to ascend
     * @param inventory the shared account inventory to deduct crystals from
     * @return AscensionResult describing the outcome
     */
    public static AscensionResult ascend(Character character, Inventory inventory) {

        // ── Guard: already fully ascended ─────────────────────────────────────
        if (character.getAscensionPhase() >= AscensionCrystal.MAX_PHASE)
            return AscensionResult.ALREADY_MAX_PHASE;

        // ── Guard: must be at the current phase cap ────────────────────────────
        // e.g. must be Lv10 to ascend to Phase 1, Lv20 for Phase 2, etc.
        int requiredLevel = (character.getAscensionPhase() + 1) * 10;
        if (character.getLevel() < requiredLevel)
            return AscensionResult.NOT_AT_LEVEL_CAP;

        // ── Find crystals in inventory ─────────────────────────────────────────
        int targetPhase = character.getAscensionPhase() + 1;
        int cost        = AscensionCrystal.costForPhase(targetPhase);

        AscensionCrystal crystal = inventory.getCrystals(character.getName()).orElse(null);
        if (crystal == null)             return AscensionResult.NO_CRYSTALS;
        if (!crystal.hasEnough(cost))    return AscensionResult.INSUFFICIENT_CRYSTALS;

        // ── Apply ascension ────────────────────────────────────────────────────
        crystal.deduct(cost);
        character.setAscensionPhase(targetPhase);
        character.applyAscensionBoost(targetPhase);

        return AscensionResult.SUCCESS;
    }

    /**
     * Returns the crystal cost for ascending a character to the next phase,
     * without performing the ascension. Useful for UI display.
     *
     * @return the cost, or 0 if the character is already max phase.
     */
    public static int nextAscensionCost(Character character) {
        int nextPhase = character.getAscensionPhase() + 1;
        if (nextPhase > AscensionCrystal.MAX_PHASE) return 0;
        return AscensionCrystal.costForPhase(nextPhase);
    }

    /**
     * The level the character must reach before they can ascend to the next phase.
     * Returns -1 if already fully ascended.
     */
    public static int requiredLevelForNextAscension(Character character) {
        if (character.getAscensionPhase() >= AscensionCrystal.MAX_PHASE) return -1;
        return (character.getAscensionPhase() + 1) * 10;
    }
}