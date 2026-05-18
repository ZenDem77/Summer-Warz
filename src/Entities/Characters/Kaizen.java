package Entities.Characters;

import Entities.Entity;
import Entities.Character;
import Combat.NormalBattle.Battle;
import Entities.Passive;

public class Kaizen extends Character {

    // ── Passive 1 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_1_DMG_MIN     = 5;
    private static final int PASSIVE_1_DMG_MAX     = 15;
    private static final int PASSIVE_1_INTERVAL_MS = 1200;

    // ── Passive 2 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_2_HEAL_MIN    = 10;
    private static final int PASSIVE_2_HEAL_MAX    = 30;
    private static final int PASSIVE_2_INTERVAL_MS = 3000;

    // ── Passive 3 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_3_BONUS_DAMAGE = 50;
    private static final int PASSIVE_3_INTERVAL_MS  = 10000;

    public Kaizen() {
        super("Kaizen", 140, 16, 4, 800, 0.05, 1.50, 1, "Iron");
    }

    @Override
    public String getSpecialMoveName() { return "Kaizen Fist"; }

    // ── Passive slots ─────────────────────────────────────────────────────────

    @Override
    public Passive[] getPassives() {
        return new Passive[]{
                passive1(),   // unlocked at level 10
                passive2(),   // unlocked at level 20
                passive3()    // unlocked at level 30
        };
    }

    private Passive passive1() {
        return new Passive() {
            @Override public int    getIntervalMs()  { return PASSIVE_1_INTERVAL_MS; }
            @Override public String getName()        { return "Chaos Strike"; }
            @Override public String getDescription() { return "Deal " + PASSIVE_1_DMG_MIN + "–" + PASSIVE_1_DMG_MAX + " random bonus damage every 1.2s (bypasses DEF)"; }

            @Override
            public void trigger(Entity owner, Battle battle) {
                int dmg = PASSIVE_1_DMG_MIN + (int)(Math.random() * (PASSIVE_1_DMG_MAX - PASSIVE_1_DMG_MIN + 1));
                Entity target = battle.getEnemy();
                target.takeDamage(dmg);
                battle.notifyPassive(owner, target, getName(),
                        dmg + " random bonus dmg (bypasses DEF)", dmg, false);
                battle.checkEndPublic();
            }
        };
    }

    private Passive passive2() {
        return new Passive() {
            @Override public int    getIntervalMs()  { return PASSIVE_2_INTERVAL_MS; }
            @Override public String getName()        { return "Iron Recovery"; }
            @Override public String getDescription() { return "Heal " + PASSIVE_2_HEAL_MIN + "–" + PASSIVE_2_HEAL_MAX + " random HP every 3s"; }

            @Override
            public void trigger(Entity owner, Battle battle) {
                int roll = PASSIVE_2_HEAL_MIN + (int)(Math.random() * (PASSIVE_2_HEAL_MAX - PASSIVE_2_HEAL_MIN + 1));
                int before = owner.getCurrentHp();
                owner.heal(roll);
                int healed = owner.getCurrentHp() - before;
                battle.notifyPassive(owner, owner, getName(),
                        "+" + healed + " HP restored", healed, true);
            }
        };
    }

    private Passive passive3() {
        return new Passive() {
            @Override public int    getIntervalMs()  { return PASSIVE_3_INTERVAL_MS; }
            @Override public String getName()        { return "Iron Wrath"; }
            @Override public String getDescription() { return "Deal " + PASSIVE_3_BONUS_DAMAGE + " flat bonus damage every 10s (bypasses DEF)"; }

            @Override
            public void trigger(Entity owner, Battle battle) {
                Entity target = battle.getEnemy();
                target.takeDamage(PASSIVE_3_BONUS_DAMAGE);
                battle.notifyPassive(owner, target, getName(),
                        PASSIVE_3_BONUS_DAMAGE + " flat bonus dmg (bypasses DEF)",
                        PASSIVE_3_BONUS_DAMAGE, false);
                battle.checkEndPublic();
            }
        };
    }

    // ── Level up ──────────────────────────────────────────────────────────────
    @Override
    public void levelUp() {
        switch (checkLevel()) {
            case 1 -> { maxHp += 4;  attack += 1; }
            case 2 -> { maxHp += 6;  attack += 2; defense += 1; }
            case 3 -> { maxHp += 8;  attack += 3; defense += 1; }
            case 4 -> { maxHp += 12; attack += 4; defense += 2; }
        }
    }

    @Override
    public String toString() { return "[" + clan + " Clan] " + super.toString(); }
}
