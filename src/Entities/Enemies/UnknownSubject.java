package Entities.Enemies;

import Entities.Enemy;
import Entities.Entity;
import Entities.PassiveHandler.*;

public class UnknownSubject extends Enemy {

    private static final double CRIT_RATE   = 0.80;
    private static final double CRIT_DAMAGE = 2.00;

    private final int tier;

    public UnknownSubject(int tier) {
        super("Unknown Subject " + tier,
                calcHp(tier),
                calcAtk(tier),
                calcDef(tier),
                calcSpeed(tier),
                CRIT_RATE,
                CRIT_DAMAGE);
        validateTier(tier);
        this.tier = tier;
    }

    // ── Stat formulas ─────────────────────────────────────────────────────────
    private static void validateTier(int tier) {
        if (tier < 1 || tier > 10)
            throw new IllegalArgumentException(
                    "UnknownSubject tier must be 1–10, got: " + tier);
    }

    private static int calcHp(int tier)    { return 150 + (tier - 1) * 900; }
    private static int calcAtk(int tier)   { return  20 + (tier - 1) * 2;  }
    private static int calcDef(int tier)   { return   2 + (tier - 1) * 2;   }
    private static int calcSpeed(int tier) { return 1000 - (tier - 1) * 50; }

    // ── Passive formulas ──────────────────────────────────────────────────────
    private static int passiveDamage(int tier)   { return  20 + (tier - 1) * 4;   }
    private static int passiveInterval(int tier) { return 3000 - (tier - 1) * 200; }

    private static String msToSec(int ms) {
        double s = ms / 1000.0;
        return (s == (int) s ? String.valueOf((int) s) : String.valueOf(s)) + "s";
    }

    // ── Passive ───────────────────────────────────────────────────────────────
    @Override
    public Passive getPassive() {
        int dmg      = passiveDamage(tier);
        int interval = passiveInterval(tier);

        return new Passive() {
            @Override public String getName()       { return "Corruption Burst"; }
            @Override public String getDescription(){ return "Deal " + dmg + " True Damage every " + msToSec(interval); }
            @Override public int    getIntervalMs() { return interval; }

            @Override
            public void trigger(PassiveContext ctx) {
                Entity target = ctx.battle.getOpponent(ctx.owner);
                int actual = Math.min(dmg, target.getCurrentHp());
                target.takeDamage(dmg);
                ctx.battle.notifyPassive(ctx.owner, target, getName(),
                        actual + " True Damage", actual, false);
                ctx.battle.checkEndPublic();
            }
        };
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public int getTier() { return tier; }

    @Override
    public String toString() {
        return "[Tier " + tier + "] " + super.toString();
    }
}