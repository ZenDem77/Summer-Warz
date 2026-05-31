package Entities;

import java.util.ArrayList;
import java.util.List;

public abstract class Character extends Entity {

    // ── Level thresholds for passive slots ───────────────────────────────────
    public static final int PASSIVE_1_LEVEL = 10;
    public static final int PASSIVE_2_LEVEL = 20;
    public static final int PASSIVE_3_LEVEL = 30;

    public static final double BASE_ACCURACY = 0.90;
    protected int level;

    public Character(String name, int maxHp, int attack, int defense, int attackSpeed, double critRate, double critDamage, int level) {
        super(name, maxHp, attack, defense, attackSpeed, critRate, critDamage, BASE_ACCURACY);
        this.level = level;
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

    public int getLevel() { return level; }
}
