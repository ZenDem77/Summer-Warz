package Entities;

import Combat.DamageResult;
import Entities.PassiveHandler.*;

public abstract class Entity {
    protected String name;
    protected int    maxHp;
    protected int    currentHp;
    protected int    attack;
    protected int    defense;
    protected int    attackSpeed;    // ms between attacks

    // ── Crit stats (owned per-instance; boosted only by that entity's weapons/items) ──
    protected double critRate;    // e.g. 0.05 = 5%
    protected double critDamage;  // e.g. 1.50 = x1.5 on crit
    protected double accuracy;    // e.g. 0.90 = 90% hit chance

    public Entity(String name, int maxHp, int attack, int defense, int attackSpeed,
                  double critRate, double critDamage, double accuracy) {
        this.name        = name;
        this.maxHp       = maxHp;
        this.currentHp   = maxHp;
        this.attack      = attack;
        this.defense     = defense;
        this.attackSpeed = attackSpeed;
        this.critRate    = critRate;
        this.critDamage  = critDamage;
        this.accuracy    = accuracy;
    }

    public DamageResult calculateDamage(Entity defender) {
        if (Math.random() >= getAccuracy()) {
            return new DamageResult(0, false, true);   // miss
        }
        boolean crit = Math.random() < getCritRate();
        int     atk  = getEffectiveAtk();
        double  raw  = crit ? atk + (atk * getCritDamage()) : atk;
        int     dmg  = Math.max(1, (int) raw - defender.getDefense());
        return new DamageResult(dmg, crit, false);
    }

    public void takeDamage(int amount) {
        currentHp = Math.max(0, currentHp - amount);
    }

    public void heal(int amount) {
        currentHp = Math.min(getMaxHp(), currentHp + amount);
    }

    public boolean isAlive() { return currentHp > 0; }

    public void reset() { currentHp = maxHp; }

    // ── Passive hook ──────────────────────────────────────────────────────────
    public Passive getPassive() { return null; }

    // ── Effective ATK ─────────────────────────────────────────────────────────
    public int getEffectiveAtk() { return attack; }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getName()        { return name; }
    public int    getMaxHp()       { return maxHp; }
    public int    getCurrentHp()   { return currentHp; }
    public int    getAttack()      { return attack; }
    public int    getDefense()     { return defense; }
    public int    getAttackSpeed() { return attackSpeed; }
    public double getCritRate()    { return critRate; }
    public double getCritDamage()  { return critDamage; }
    public double getAccuracy()    { return accuracy; }
    public double getHpPercent()   { return (double) currentHp / getMaxHp(); }

    // ── Crit stat modifiers (called by weapons/items) ─────────────────────────
    public void addCritRate(double bonus)   { critRate   += bonus; }
    public void addCritDamage(double bonus) { critDamage += bonus; }
    public void addAccuracy(double bonus)   { accuracy   += bonus; }

    @Override
    public String toString() {
        return name + " [HP: " + currentHp + "/" + maxHp + "]";
    }
}