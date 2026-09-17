package Entities;

import Combat.Passive;

import java.util.ArrayList;
import java.util.List;

public abstract class Character extends Entity {

    // ── Level thresholds for passive slots ───────────────────────────────────
    public static final int PASSIVE_1_LEVEL = 10;
    public static final int PASSIVE_2_LEVEL = 20;
    public static final int PASSIVE_3_LEVEL = 30;

    protected int    level;
    protected int    karma;
    protected String clan;

    public Character(String name, int maxHp, int attack, int defense,
                     int attackSpeed, int level, String clan) {
        super(name, maxHp, attack, defense, attackSpeed);
        this.level = level;
        this.karma = 0;
        this.clan  = clan;
    }

    // ── Karma ─────────────────────────────────────────────────────────────────

    public void gainKarma(int amount) { this.karma += amount; }

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

    public int    getLevel() { return level; }
    public int    getKarma() { return karma; }
    public String getClan()  { return clan; }

    public abstract String getSpecialMoveName();
}