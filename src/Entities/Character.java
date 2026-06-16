package Entities;

import Entities.Artifacts.Artifact;
import Entities.PassiveHandler.Passive;

import java.util.ArrayList;
import java.util.List;

public abstract class Character extends Entity {

    // ── Level thresholds for passive slots ───────────────────────────────────
    public static final int PASSIVE_1_LEVEL = 10;
    public static final int PASSIVE_2_LEVEL = 20;
    public static final int PASSIVE_3_LEVEL = 30;

    // ── Level thresholds for artifact slots ──────────────────────────────────
    public static final int ARTIFACT_SLOT_1_LEVEL = 1;
    public static final int ARTIFACT_SLOT_2_LEVEL = 10;
    public static final int ARTIFACT_SLOT_3_LEVEL = 20;
    public static final int ARTIFACT_SLOT_4_LEVEL = 30;

    private static final int[] ARTIFACT_SLOT_UNLOCK_LEVELS = {
            ARTIFACT_SLOT_1_LEVEL, ARTIFACT_SLOT_2_LEVEL, ARTIFACT_SLOT_3_LEVEL, ARTIFACT_SLOT_4_LEVEL
    };

    public static final double BASE_ACCURACY = 0.80;
    public static final int ARTIFACT_SLOT_COUNT = 4;
    protected int level;

    // ── Weapon contribution ───────────────────────────────────────────────────
    private int      weaponAtk           = 0;
    private StatType weaponSecondaryType  = null;   // null = no secondary stat
    private double   weaponSecondaryValue = 0.0;

    private int    artifactFlatAtk    = 0;
    private double artifactAtkPercent = 0.0;

    // ── Passive ATK bonus ─────────────────────────────────────────────────────
    private int passiveAtkBonus = 0;

    // ── Artifact equip slots ──────────────────────────────────────────────────
    private final Artifact[] artifactSlots = new Artifact[ARTIFACT_SLOT_COUNT];

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
        int    base       = getBaseAtk();
        int    flatBonus  = artifactFlatAtk;     // legacy manual override (default 0)
        double atkPercent = artifactAtkPercent;  // legacy manual override (default 0)

        // Weapon secondary ATK / ATK%
        if (weaponSecondaryType == StatType.ATK) {
            flatBonus += (int) weaponSecondaryValue;
        }
        if (weaponSecondaryType == StatType.ATK_PERCENT) {
            atkPercent += weaponSecondaryValue;
        }

        // Artifact substats — summed across all 4 equip slots
        flatBonus  += (int) getArtifactSubstatTotal(StatType.ATK);
        atkPercent += getArtifactSubstatTotal(StatType.ATK_PERCENT);

        return (int)(base * (1.0 + atkPercent)) + flatBonus + passiveAtkBonus;
    }

    @Override
    public int getEffectiveAtk() { return getTotalAtk(); }

    // ── Weapon integration ────────────────────────────────────────────────────
    public void applyWeaponStats(int primaryAtk, StatType secondaryType, double secondaryValue) {
        removeWeaponStats();   // cleanly remove any currently equipped weapon first
        this.weaponAtk            = primaryAtk;
        this.weaponSecondaryType  = secondaryType;
        this.weaponSecondaryValue = secondaryValue;

        if (secondaryType != null && secondaryType != StatType.ATK && secondaryType != StatType.ATK_PERCENT) {
            applyStatBonus(secondaryType, secondaryValue);
        }
    }

    public void removeWeaponStats() {
        if (weaponSecondaryType != null
                && weaponSecondaryType != StatType.ATK
                && weaponSecondaryType != StatType.ATK_PERCENT) {
            removeStatBonus(weaponSecondaryType, weaponSecondaryValue);
        }
        weaponAtk            = 0;
        weaponSecondaryType  = null;
        weaponSecondaryValue = 0.0;
    }

    // ── Legacy manual artifact ATK override ───────────────────────────────────
    public void applyArtifactAtkBonus(int flatAtk, double atkPercent) {
        this.artifactFlatAtk    = flatAtk;
        this.artifactAtkPercent = atkPercent;
    }

    public void removeArtifactAtkBonus() {
        this.artifactFlatAtk    = 0;
        this.artifactAtkPercent = 0.0;
    }

    // ── Passive ATK bonus (for self-buffing passives) ────────────────────────
    public void addPassiveAtkBonus(int amount) { passiveAtkBonus += amount; }

    public void removePassiveAtkBonus(int amount) { passiveAtkBonus -= amount; }

    public int getPassiveAtkBonus() { return passiveAtkBonus; }

    // ── Artifact equip slots ───────────────────────────────────────────────────
    public void equipArtifact(int slot, Artifact artifact) {
        validateSlot(slot);
        if (artifact == null)
            throw new IllegalArgumentException("Use unequipArtifact() to clear a slot, not equipArtifact(null).");
        if (!isSlotUnlocked(slot))
            throw new IllegalStateException(
                    name + " must be level " + getSlotUnlockLevel(slot)
                            + " to use artifact slot " + (slot + 1) + " (currently level " + level + ").");
        artifactSlots[slot] = artifact;
        currentHp = Math.min(currentHp, getMaxHp());
    }

    public void unequipArtifact(int slot) {
        validateSlot(slot);
        artifactSlots[slot] = null;
        currentHp = Math.min(currentHp, getMaxHp());
    }

    public Artifact getArtifact(int slot) {
        validateSlot(slot);
        return artifactSlots[slot];
    }

    public Artifact[] getArtifactSlots() {
        return artifactSlots.clone();
    }

    public boolean isSlotUnlocked(int slot) {
        validateSlot(slot);
        return level >= ARTIFACT_SLOT_UNLOCK_LEVELS[slot];
    }

    public static int getSlotUnlockLevel(int slot) {
        validateSlot(slot);
        return ARTIFACT_SLOT_UNLOCK_LEVELS[slot];
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= ARTIFACT_SLOT_COUNT)
            throw new IllegalArgumentException("Artifact slot must be 0-" + (ARTIFACT_SLOT_COUNT - 1) + ", got: " + slot);
    }

    public double getArtifactSubstatTotal(StatType type) {
        double total = 0.0;
        for (Artifact a : artifactSlots) {
            if (a != null) total += a.getSubstatValue(type);
        }
        return total;
    }

    // ── Stat overrides (include artifact substat contributions) ──────────────
    @Override
    public double getCritRate() {
        return super.getCritRate() + getArtifactSubstatTotal(StatType.CRIT_RATE);
    }

    @Override
    public double getCritDamage() {
        return super.getCritDamage() + getArtifactSubstatTotal(StatType.CRIT_DAMAGE);
    }

    @Override
    public double getAccuracy() {
        return super.getAccuracy() + getArtifactSubstatTotal(StatType.ACCURACY);
    }

    @Override
    public double getDamageBonus() {
        return super.getDamageBonus() + getArtifactSubstatTotal(StatType.DAMAGE_BONUS);
    }

    @Override
    public int getDefense() {
        int    baseDef    = super.getDefense();
        double defPercent = getArtifactSubstatTotal(StatType.DEF_PERCENT);
        int    flatDef    = (int) getArtifactSubstatTotal(StatType.DEF);
        return (int)(baseDef * (1.0 + defPercent)) + flatDef;
    }

    @Override
    public int getMaxHp() {
        int    baseMaxHp = super.getMaxHp();
        double hpPercent = getArtifactSubstatTotal(StatType.HP_PERCENT);
        int    flatHp    = (int) getArtifactSubstatTotal(StatType.HP);
        return (int)(baseMaxHp * (1.0 + hpPercent)) + flatHp;
    }

    @Override
    public void reset() {
        currentHp = getMaxHp();
    }

    // ── Generic stat bonus helpers ────────────────────────────────────────────
    public void applyStatBonus(StatType type, double value) {
        switch (type) {
            case CRIT_RATE    -> addCritRate(value);
            case CRIT_DAMAGE  -> addCritDamage(value);
            case ACCURACY     -> addAccuracy(value);
            case DAMAGE_BONUS -> addDamageBonus(value);
            case DEF          -> defense += (int) value;
            case HP          -> {
                maxHp     += (int) value;
                currentHp  = Math.min(currentHp + (int) value, maxHp);
            }
            case ATK, ATK_PERCENT, DEF_PERCENT, HP_PERCENT -> { /* handled via getters */ }
        }
    }

    public void removeStatBonus(StatType type, double value) {
        switch (type) {
            case CRIT_RATE    -> critRate    -= value;
            case CRIT_DAMAGE  -> critDamage  -= value;
            case ACCURACY     -> accuracy    -= value;
            case DAMAGE_BONUS -> damageBonus -= value;
            case DEF          -> defense     -= (int) value;
            case HP          -> {
                maxHp     -= (int) value;
                currentHp  = Math.min(currentHp, maxHp);
            }
            case ATK, ATK_PERCENT, DEF_PERCENT, HP_PERCENT -> { /* handled via getters */ }
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

    // ── Display ───────────────────────────────────────────────────────────────
    public String getSummary() {
        String base = name + "\nLevel: " + level + "\nHp: " + getMaxHp() + "\nAtk: " + getEffectiveAtk() +
                "\nDef: " + getDefense() + "\nCrit Rate: " + (getCritRate() * 100) + "%" +
                "\nCrit Damage: " + (getCritDamage() * 100) + "%" +
                "\nDamage Bonus: " + (getDamageBonus() * 100) + "%" +
                "\nAccuracy: " + (getAccuracy() * 100) + "%";
        return base;
    }
}