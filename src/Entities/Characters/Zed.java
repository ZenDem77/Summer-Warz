package Entities.Characters;

import Combat.IBattle;
import Entities.Entity;
import Entities.Character;
import Entities.Passive;
import Entities.PassiveContext;

public class Zed extends Character {

    // ── Passive 1 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_1_BONUS_DAMAGE = 20;
    private static final int PASSIVE_1_INTERVAL_MS  = 5000;

    // ── Passive 2 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_2_BONUS_DAMAGE = 5;
    private static final int PASSIVE_2_INTERVAL_MS  = 1500;

    // ── Passive 3 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_3_BONUS_DAMAGE = 3;
    private static final int PASSIVE_3_HEAL_AMOUNT  = 10;
    private static final int PASSIVE_3_INTERVAL_MS  = 3000;

    public Zed() {
        super("Zed", 120, 27, 5, 700, 0.05, 0.50, 1, "Shadow");
    }

    @Override
    public String getSpecialMoveName() { return "Shadow Strike"; }

    // ── Passive slots ─────────────────────────────────────────────────────────
    @Override
    public Passive[] getPassives() {
        return new Passive[]{ passive1(), passive2(), passive3() };
    }

    private Passive passive1() {
        return new Passive() {
            @Override public String getName()        { return "Shadow Surge"; }
            @Override public String getDescription() { return "Deal " + PASSIVE_1_BONUS_DAMAGE + " bonus dmg every 5s (bypasses DEF)"; }
            @Override public int    getIntervalMs()  { return PASSIVE_1_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                Entity target = ctx.battle.getOpponent(ctx.owner);
                target.takeDamage(PASSIVE_1_BONUS_DAMAGE);
                ctx.battle.notifyPassive(ctx.owner, target, getName(),
                        PASSIVE_1_BONUS_DAMAGE + " bonus dmg (bypasses DEF)",
                        PASSIVE_1_BONUS_DAMAGE, false);
                ctx.battle.checkEndPublic();
            }
        };
    }

    private Passive passive2() {
        return new Passive() {
            @Override public String getName()        { return "Dark Echo"; }
            @Override public String getDescription() { return "Deal " + PASSIVE_2_BONUS_DAMAGE + " bonus dmg every 1.5s (bypasses DEF)"; }
            @Override public int    getIntervalMs()  { return PASSIVE_2_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                Entity target = ctx.battle.getOpponent(ctx.owner);
                target.takeDamage(PASSIVE_2_BONUS_DAMAGE);
                ctx.battle.notifyPassive(ctx.owner, target, getName(),
                        PASSIVE_2_BONUS_DAMAGE + " bonus dmg (bypasses DEF)",
                        PASSIVE_2_BONUS_DAMAGE, false);
                ctx.battle.checkEndPublic();
            }
        };
    }

    private Passive passive3() {
        return new Passive() {
            @Override public String getName()        { return "Shadow Mend"; }
            @Override public String getDescription() { return "Deal " + PASSIVE_3_BONUS_DAMAGE + " bonus dmg and heal " + PASSIVE_3_HEAL_AMOUNT + " HP every 3s"; }
            @Override public int    getIntervalMs()  { return PASSIVE_3_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                Entity target = ctx.battle.getOpponent(ctx.owner);
                target.takeDamage(PASSIVE_3_BONUS_DAMAGE);
                ctx.battle.notifyPassive(ctx.owner, target, getName(),
                        PASSIVE_3_BONUS_DAMAGE + " bonus dmg (bypasses DEF)",
                        PASSIVE_3_BONUS_DAMAGE, false);
                ctx.battle.checkEndPublic();

                if (ctx.owner.isAlive()) {
                    int before = ctx.owner.getCurrentHp();
                    ctx.owner.heal(PASSIVE_3_HEAL_AMOUNT);
                    int healed = ctx.owner.getCurrentHp() - before;
                    ctx.battle.notifyPassive(ctx.owner, ctx.owner, getName(),
                            "+" + healed + " HP restored", healed, true);
                }
            }
        };
    }

    // ── Level up ──────────────────────────────────────────────────────────────
    @Override
    public void levelUp() {
        switch (checkLevel()) {
            case 1 -> { maxHp += 2;  attack += 1; }
            case 2 -> { maxHp += 4;  attack += 2; defense += 1; }
            case 3 -> { maxHp += 6;  attack += 3; defense += 1; }
            case 4 -> { maxHp += 10; attack += 4; defense += 2; }
        }
    }

    @Override
    public String toString() { return "[" + clan + " Clan] " + super.toString(); }
}