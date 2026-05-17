package Entities;

import Combat.DamageResult;
import Combat.Passive;

public abstract class Entity {
    protected String name;
    protected int    maxHp;
    protected int    currentHp;
    protected int    attack;
    protected int    defense;
    protected int    attackSpeed;    // ms between attacks

    // ── Crit stats (base values; boosted by weapons/items later) ─────────────
    protected double critRate   = 0.05;   // 5%
    protected double critDamage = 1.50;   // +50% → ×1.5 total

    public Entity(String name, int maxHp, int attack, int defense, int attackSpeed) {
        this.name        = name;
        this.maxHp       = maxHp;
        this.currentHp   = maxHp;
        this.attack      = attack;
        this.defense     = defense;
        this.attackSpeed = attackSpeed;
    }

    public DamageResult calculateDamage(Entity defender) {
        boolean crit   = Math.random() < critRate;
        double  raw    = crit ? attack * critDamage : attack;
        int     dmg    = Math.max(0, (int) raw - defender.getDefense());
        return new DamageResult(dmg, crit);
    }

    public void takeDamage(int amount) {
        currentHp = Math.max(0, currentHp - amount);
    }

    public void heal(int amount) {
        currentHp = Math.min(maxHp, currentHp + amount);
    }

    public boolean isAlive() { return currentHp > 0; }

    public void reset() { currentHp = maxHp; }

    // ── Passive hook ──────────────────────────────────────────────────────────
    public Passive getPassive() { return null; }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getName()        { return name; }
    public int    getMaxHp()       { return maxHp; }
    public int    getCurrentHp()   { return currentHp; }
    public int    getAttack()      { return attack; }
    public int    getDefense()     { return defense; }
    public int    getAttackSpeed() { return attackSpeed; }
    public double getCritRate()    { return critRate; }
    public double getCritDamage()  { return critDamage; }

    public double getHpPercent()   { return (double) currentHp / maxHp; }

    // ── Crit stat modifiers (called by weapons/items) ─────────────────────────
    public void addCritRate(double bonus)   { critRate   += bonus; }
    public void addCritDamage(double bonus) { critDamage += bonus; }

    @Override
    public String toString() {
        return name + " [HP: " + currentHp + "/" + maxHp + "]";
    }
}