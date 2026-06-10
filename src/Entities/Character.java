package Entities;

import Entities.PassiveHandler.Passive;

import java.util.ArrayList;
import java.util.List;

public abstract class Character extends Entity {

    // ── Level thresholds for passive slots ───────────────────────────────────
    public static final int PASSIVE_1_LEVEL = 10;
    public static final int PASSIVE_2_LEVEL = 20;
    public static final int PASSIVE_3_LEVEL = 30;

    public static final double BASE_ACCURACY = 0.85;
    protected int level;

    // ── Weapon contribution ───────────────────────────────────────────────────
    private int      weaponAtk           = 0;
    private StatType weaponSecondaryType  = null;   // null = no secondary stat
    private double   weaponSecondaryValue = 0.0;

    // ── Artifact contribution ─────────────────────────────────────────────────
    private int    artifactFlatAtk    = 0;
    private double artifactAtkPercent = 0.0;

    // ─────────────────────────────────────────────────────────────────────────
    public Character(String name, int maxHp, int attack, int defense, int attackSpeed, double critRate, double critDamage, int level) {
        super(name, maxHp, attack, defense, attackSpeed, critRate, critDamage, BASE_ACCURACY);
        this.level = level;
    }

    // ── ATK breakdown ─────────────────────────────────────────────────────────
    public int getBaseAtk() {
        return attack + weaponAtk;
    }

    public int getTotalAtk() {
        int base     = getBaseAtk();
        int flatBonus = artifactFlatAtk;

        // Weapon secondary ATK is a flat bonus, not part of base ATK
        if (weaponSecondaryType == StatType.ATK) {
            flatBonus += (int) weaponSecondaryValue;
        }

        return (int)(base * (1.0 + artifactAtkPercent)) + flatBonus;
    }

    @Override
    public int getEffectiveAtk() { return getTotalAtk(); }

    // ── Weapon integration (called by future Weapon class) ────────────────────
    public void applyWeaponStats(int primaryAtk, StatType secondaryType, double secondaryValue) {
        removeWeaponStats();   // cleanly remove any currently equipped weapon first
        this.weaponAtk            = primaryAtk;
        this.weaponSecondaryType  = secondaryType;
        this.weaponSecondaryValue = secondaryValue;

        if (secondaryType != null && secondaryType != StatType.ATK) {
            applyStatBonus(secondaryType, secondaryValue);
        }
    }

    public void removeWeaponStats() {
        if (weaponSecondaryType != null && weaponSecondaryType != StatType.ATK) {
            removeStatBonus(weaponSecondaryType, weaponSecondaryValue);
        }
        weaponAtk            = 0;
        weaponSecondaryType  = null;
        weaponSecondaryValue = 0.0;
    }

    // ── Artifact ATK integration (called by future artifact system) ───────────
    public void applyArtifactAtkBonus(int flatAtk, double atkPercent) {
        this.artifactFlatAtk    = flatAtk;
        this.artifactAtkPercent = atkPercent;
    }

    public void removeArtifactAtkBonus() {
        this.artifactFlatAtk    = 0;
        this.artifactAtkPercent = 0.0;
    }

    // ── Generic stat bonus helpers ────────────────────────────────────────────
    public void applyStatBonus(StatType type, double value) {
        switch (type) {
            case CRIT_RATE   -> addCritRate(value);
            case CRIT_DAMAGE -> addCritDamage(value);
            case ACCURACY    -> addAccuracy(value);
            case DEF         -> defense += (int) value;
            case HP          -> {
                maxHp     += (int) value;
                currentHp  = Math.min(currentHp + (int) value, maxHp);
            }
            case ATK, ATK_PERCENT -> { /* handled via getTotalAtk() */ }
        }
    }

    public void removeStatBonus(StatType type, double value) {
        switch (type) {
            case CRIT_RATE   -> critRate   -= value;
            case CRIT_DAMAGE -> critDamage -= value;
            case ACCURACY    -> accuracy   -= value;
            case DEF         -> defense    -= (int) value;
            case HP          -> {
                maxHp     -= (int) value;
                currentHp  = Math.min(currentHp, maxHp);
            }
            case ATK, ATK_PERCENT -> { /* handled via getTotalAtk() */ }
        }
    }

    // ── Passive slots ─────────────────────────────────────────────────────────
    public Passive[] getPassives() {
        return new Passive[]{ null, null, null };
    }

    public List<Passive> getActivePassives() {
        Passive[] slots = getPassives();
        List<Passive> active = new ArrayList<>();
        if (level >= PASSIVE_1_LEVEL && slots[0] != null) active.add(slots[0]);
        if (level >= PASSIVE_2_LEVEL && slots[1] != null) active.add(slots[1]);
        if (level >= PASSIVE_3_LEVEL && slots[2] != null) active.add(slots[2]);
        return active;
    }

    // ── Level up ──────────────────────────────────────────────────────────────
    public int checkLevel() {
        if      (level < PASSIVE_1_LEVEL) return 1;
        else if (level < PASSIVE_2_LEVEL) return 2;
        else if (level < PASSIVE_3_LEVEL) return 3;
        else                              return 4;
    }

    public void gainLevel() {
        level++;
        levelUp();
    }

    public abstract void levelUp();

    // ── Getters ───────────────────────────────────────────────────────────────
    public int      getLevel()               { return level; }
    public int      getWeaponAtk()           { return weaponAtk; }
    public StatType getWeaponSecondaryType() { return weaponSecondaryType; }
    public double   getWeaponSecondaryValue(){ return weaponSecondaryValue; }
    public int      getArtifactFlatAtk()     { return artifactFlatAtk; }
    public double   getArtifactAtkPercent()  { return artifactAtkPercent; }
}
