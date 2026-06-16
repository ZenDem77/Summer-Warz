package Economy;

/**
 * CharacterLevelCost — Elixir cost to level up a character by one level.
 *
 * Cost is bracketed every 5 levels, based on the character's CURRENT level
 * (before leveling up).
 *
 * ── TO CUSTOMIZE ─────────────────────────────────────────────────────────
 * Just edit the numbers in COST_PER_BRACKET below. Levels beyond the last
 * defined bracket automatically reuse the LAST cost in the array, so you
 * never need to worry about "running out" of brackets — just add more
 * entries if you want costs to keep climbing further.
 *
 * Bracket mapping (character levels start at 1, no fixed cap):
 *   index 0 → levels  1– 5
 *   index 1 → levels  6–10
 *   index 2 → levels 11–15
 *   index 3 → levels 16–20
 *   index 4 → levels 21–25
 *   index 5 → levels 26–30
 *   (levels 31+ reuse the cost at index 5)
 */
public class CharacterLevelCost {

    private static final int[] COST_PER_BRACKET = {
            20,     // levels  1– 5
            60,     // levels  6–10
            150,    // levels 11–15
            300,    // levels 16–20
            500,    // levels 21–25
            800,    // levels 26–30
    };

    /** Returns the Elixir cost to level up from the given current level. */
    public static int getCost(int currentLevel) {
        int bracket = (currentLevel - 1) / 5;
        bracket = Math.min(bracket, COST_PER_BRACKET.length - 1);
        return COST_PER_BRACKET[bracket];
    }
}