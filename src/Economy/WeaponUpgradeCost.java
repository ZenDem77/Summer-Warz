package Economy;

/**
 * WeaponUpgradeCost — Elixir cost to ATTEMPT a weapon upgrade.
 *
 * Cost is bracketed every 5 levels, based on the weapon's CURRENT level
 * (before the attempt) — matches the same bracket pattern as the weapon's
 * upgrade success-rate tiers.
 *
 * ── TO CUSTOMIZE ─────────────────────────────────────────────────────────
 * Just edit the numbers in COST_PER_BRACKET below. Nothing else needs to
 * change — getCost() automatically maps any level into the right bracket.
 *
 * Bracket mapping (weapon levels run 0–20):
 *   index 0 → levels  0– 4
 *   index 1 → levels  5– 9
 *   index 2 → levels 10–14
 *   index 3 → levels 15–19
 *   index 4 → level     20   (max level — Weapon.canUpgrade() is false here anyway)
 */
public class WeaponUpgradeCost {

    private static final int[] COST_PER_BRACKET = {
            50,     // levels  0– 4
            150,    // levels  5– 9
            400,    // levels 10–14
            900,    // levels 15–19
            0       // level     20 — no further upgrades possible
    };

    /** Returns the Elixir cost to attempt an upgrade from the given current level. */
    public static int getCost(int currentLevel) {
        int bracket = Math.min(currentLevel / 5, COST_PER_BRACKET.length - 1);
        return COST_PER_BRACKET[bracket];
    }
}