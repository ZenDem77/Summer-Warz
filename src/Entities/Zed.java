package Entities;

import Combat.Battle;
import Combat.Passive;

public class Zed extends Character {

    // ── Passive 1 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_1_BONUS_DAMAGE = 20;
    private static final int PASSIVE_1_INTERVAL_MS  = 5000;

    public Zed() {
        super("Zed", 120, 18, 5, 700, 1, "Shadow");
    }

    @Override
    public String getSpecialMoveName() { return "Shadow Strike"; }

    // ── Passive slots ─────────────────────────────────────────────────────────
    @Override
    public Passive[] getPassives() {
        return new Passive[]{
                passive1(),   // unlocked at level 10
                null,         // unlocked at level 20 — TBD
                null          // unlocked at level 30 — TBD
        };
    }

    private Passive passive1() {
        return new Passive() {
            @Override public int    getIntervalMs()  { return PASSIVE_1_INTERVAL_MS; }
            @Override public String getName()        { return "Shadow Surge"; }
            @Override public String getDescription() { return "Deal " + PASSIVE_1_BONUS_DAMAGE + " bonus damage every 5s (bypasses DEF)"; }

            @Override
            public void trigger(Entity owner, Battle battle) {
                Entity target = battle.getEnemy();
                target.takeDamage(PASSIVE_1_BONUS_DAMAGE);
                battle.notifyPassive(owner, target, getName(),
                        PASSIVE_1_BONUS_DAMAGE + " bonus dmg (bypasses DEF)",
                        PASSIVE_1_BONUS_DAMAGE, false);
                battle.checkEndPublic();
            }
        };
    }

    // ── Level up ──────────────────────────────────────────────────────────────
    @Override
    public void levelUp() {
        switch (checkLevel()) {
            case 1 -> { maxHp += 3;  attack += 1; }
            case 2 -> { maxHp += 5;  attack += 2; defense += 1; }
            case 3 -> { maxHp += 7;  attack += 3; defense += 1; }
            case 4 -> { maxHp += 10; attack += 4; defense += 2; }
        }
    }

    @Override
    public String toString() { return "[" + clan + " Clan] " + super.toString(); }
}