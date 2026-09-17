package Entities.Weapons;

import Entities.Character;
import Entities.StatType;

public abstract class Weapon {

    // ── Upgrade constants ────────────────────────────────────────────────────
    public static final int MAX_LEVEL = 20;

    private static final java.util.Set<Integer> SAFE_FLOORS = java.util.Set.of(1, 5, 10, 15);

    // ── Identity ─────────────────────────────────────────────────────────────
    protected final String   name;

    // ── Base stats (at level 0) ──────────────────────────────────────────────
    protected final int      baseAtk;
    protected final StatType secondaryStatType;        // null if weapon has no secondary
    protected final double   baseSecondaryValue;        // 0.0 if no secondary

    // ── Scaling per upgrade ───────────────────────────────────────────────────
    protected final int    atkPerLevel;
    protected final double secondaryPerMilestone;

    // ── Current upgrade state ─────────────────────────────────────────────────
    private int weaponLevel = 0;   // +0 to +20

    private Character equippedOn = null;

    // ── Constructors ──────────────────────────────────────────────────────────

    protected Weapon(String name, int baseAtk, int atkPerLevel,
                     StatType secondaryStatType, double baseSecondaryValue,
                     double secondaryPerMilestone) {
        this.name                  = name;
        this.baseAtk               = baseAtk;
        this.atkPerLevel           = atkPerLevel;
        this.secondaryStatType     = secondaryStatType;
        this.baseSecondaryValue    = baseSecondaryValue;
        this.secondaryPerMilestone = secondaryPerMilestone;
    }

    // ── Current stat values (base + level scaling) ───────────────────────────

    public int getCurrentAtk() {
        return baseAtk + atkPerLevel * weaponLevel;
    }

    public double getCurrentSecondaryValue() {
        if (secondaryStatType == null) return 0.0;
        int milestonesReached = weaponLevel / 5;   // integer division: 0-4→0, 5-9→1, 10-14→2, 15-19→3, 20→4
        return baseSecondaryValue + secondaryPerMilestone * milestonesReached;
    }

    // ── Equip / Unequip ───────────────────────────────────────────────────────

    public void equip(Character character) {
        if (equippedOn != null) unequip();
        character.applyWeaponStats(getCurrentAtk(), secondaryStatType, getCurrentSecondaryValue());
        equippedOn = character;
    }

    public void unequip() {
        if (equippedOn != null) {
            equippedOn.removeWeaponStats();
            equippedOn = null;
        }
    }

    private void refreshEquippedStats() {
        if (equippedOn != null) {
            equippedOn.removeWeaponStats();
            equippedOn.applyWeaponStats(getCurrentAtk(), secondaryStatType, getCurrentSecondaryValue());
        }
    }

    // ── Upgrade system ────────────────────────────────────────────────────────

    public enum UpgradeResult { SUCCESS, FAILURE_DECREMENTED, FAILURE_SAFE, MAX_LEVEL_REACHED }

    public double getUpgradeSuccessChance() {
        if (weaponLevel >= MAX_LEVEL) return 0.0;
        if (weaponLevel < 5)  return 1.00;
        if (weaponLevel < 10) return 0.80;
        if (weaponLevel < 15) return 0.60;
        return 0.40;   // 15-19
    }

    public boolean canUpgrade() {
        return weaponLevel < MAX_LEVEL;
    }

    public boolean isSafeFloor() {
        return SAFE_FLOORS.contains(weaponLevel);
    }

    public UpgradeResult attemptUpgrade() {
        if (!canUpgrade()) return UpgradeResult.MAX_LEVEL_REACHED;

        double chance = getUpgradeSuccessChance();
        boolean success = Math.random() < chance;

        UpgradeResult result;
        if (success) {
            weaponLevel++;
            result = UpgradeResult.SUCCESS;
        } else if (isSafeFloor()) {
            // level unchanged
            result = UpgradeResult.FAILURE_SAFE;
        } else {
            weaponLevel = Math.max(0, weaponLevel - 1);
            result = UpgradeResult.FAILURE_DECREMENTED;
        }

        refreshEquippedStats();
        return result;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    public boolean   isEquipped()      { return equippedOn != null; }
    public Character getEquippedOn()   { return equippedOn; }
    public boolean   hasSecondaryStat(){ return secondaryStatType != null; }
    public int       getWeaponLevel()  { return weaponLevel; }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String   getName()                { return name; }
    public int      getBaseAtk()             { return baseAtk; }
    public int      getAtkPerLevel()         { return atkPerLevel; }
    public StatType getSecondaryStatType()   { return secondaryStatType; }
    public double   getBaseSecondaryValue()  { return baseSecondaryValue; }
    public double   getSecondaryPerMilestone(){ return secondaryPerMilestone; }

    // ── Display ───────────────────────────────────────────────────────────────

    public String getSummary() {
        String base = name + " +" + weaponLevel + "  [ATK +" + getCurrentAtk() + "]";
        if (secondaryStatType == null) return base;

        double secVal = getCurrentSecondaryValue();
        String secStr = switch (secondaryStatType) {
            case CRIT_RATE, CRIT_DAMAGE, ACCURACY, ATK_PERCENT ->
                    secondaryStatType.name().replace("_", " ")
                            + " +" + (int)(secVal * 100) + "%";
            case ATK, DEF, HP ->
                    secondaryStatType.name() + " +" + (int) secVal;
        };
        return base + "  |  " + secStr;
    }

    @Override
    public String toString() { return getSummary(); }
}