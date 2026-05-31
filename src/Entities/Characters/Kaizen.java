package Entities.Characters;

import Combat.IBattle;
import Entities.Entity;
import Entities.Character;
import Entities.Passive;
import Entities.PassiveContext;

public class Kaizen extends Character {

    // ── Passive 1 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_1_DMG_MIN     = 5;
    private static final int PASSIVE_1_DMG_MAX     = 15;
    private static final int PASSIVE_1_INTERVAL_MS = 1000;

    // ── Passive 2 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_2_HEAL_MIN    = 10;
    private static final int PASSIVE_2_HEAL_MAX    = 30;
    private static final int PASSIVE_2_INTERVAL_MS = 2000;

    // ── Passive 3 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_3_BONUS_DAMAGE = 50;
    private static final int PASSIVE_3_INTERVAL_MS  = 4000;

    public Kaizen() {
        super("Kaizen", 340, 16, 4, 500, 0.05, 0.50, 1);
    }

    // ── Passive slots ─────────────────────────────────────────────────────────
    @Override
    public Passive[] getPassives() {
        return new Passive[]{ passive1(), passive2(), passive3() };
    }

    private static String msToSec(int ms) {
        double s = ms / 1000.0;
        return (s == (int) s ? String.valueOf((int) s) : String.valueOf(s)) + "s";
    }

    private Passive passive1() {
        return new Passive() {
            @Override public String getName()        { return "Chaos Strike"; }
            @Override public String getDescription() { return "Deal " + PASSIVE_1_DMG_MIN + "–" + PASSIVE_1_DMG_MAX + " random bonus dmg every " + msToSec(PASSIVE_1_INTERVAL_MS) + " (bypasses DEF)"; }
            @Override public int    getIntervalMs()  { return PASSIVE_1_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                int dmg = PASSIVE_1_DMG_MIN + (int)(Math.random() * (PASSIVE_1_DMG_MAX - PASSIVE_1_DMG_MIN + 1));
                Entity target = ctx.battle.getOpponent(ctx.owner);
                int actual = Math.min(dmg, target.getCurrentHp());
                target.takeDamage(dmg);
                ctx.battle.notifyPassive(ctx.owner, target, getName(),
                        actual + " random bonus dmg (bypasses DEF)", actual, false);
                ctx.battle.checkEndPublic();
            }
        };
    }

    private Passive passive2() {
        return new Passive() {
            @Override public String getName()        { return "Iron Recovery"; }
            @Override public String getDescription() { return "Heal " + PASSIVE_2_HEAL_MIN + "–" + PASSIVE_2_HEAL_MAX + " random HP every " + msToSec(PASSIVE_2_INTERVAL_MS); }
            @Override public int    getIntervalMs()  { return PASSIVE_2_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                int roll = PASSIVE_2_HEAL_MIN + (int)(Math.random() * (PASSIVE_2_HEAL_MAX - PASSIVE_2_HEAL_MIN + 1));
                int before = ctx.owner.getCurrentHp();
                ctx.owner.heal(roll);
                int healed = ctx.owner.getCurrentHp() - before;
                ctx.battle.notifyPassive(ctx.owner, ctx.owner, getName(),
                        "+" + healed + " HP restored", healed, true);
            }
        };
    }

    private Passive passive3() {
        return new Passive() {
            @Override public String getName()        { return "Iron Wrath"; }
            @Override public String getDescription() { return "Deal " + PASSIVE_3_BONUS_DAMAGE + " flat bonus dmg every " + msToSec(PASSIVE_3_INTERVAL_MS) + " (bypasses DEF)"; }
            @Override public int    getIntervalMs()  { return PASSIVE_3_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                Entity target = ctx.battle.getOpponent(ctx.owner);
                int actual = Math.min(PASSIVE_3_BONUS_DAMAGE, target.getCurrentHp());
                target.takeDamage(PASSIVE_3_BONUS_DAMAGE);
                ctx.battle.notifyPassive(ctx.owner, target, getName(),
                        actual + " flat bonus dmg (bypasses DEF)",
                        actual, false);
                ctx.battle.checkEndPublic();
            }
        };
    }

    // ── Level up ──────────────────────────────────────────────────────────────
    @Override
    public void levelUp() {
        switch (checkLevel()) {
            case 1 -> { maxHp += 4;  attack += 1; }
            case 2 -> { maxHp += 6;  attack += 2; }
            case 3 -> { maxHp += 8;  attack += 3; }
            case 4 -> { maxHp += 12; attack += 4; defense += 1; }
        }
    }

    @Override
    public String toString() { return super.toString(); }
}