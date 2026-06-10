package Entities.Enemies;

import Entities.Enemy;
import Entities.PassiveHandler.*;

import java.util.EnumSet;
import java.util.Set;

public class ExperimentalSubject extends Enemy {

    private static final double CRIT_RATE      = 0.80;
    private static final double CRIT_DAMAGE    = 2.00;
    private static final double DAMAGE_REDUCE  = 0.10;

    private final int tier;

    public ExperimentalSubject(int tier) {
        super("Experimental Subject " + tier,
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
                    "ExperimentalSubject tier must be 1–10, got: " + tier);
    }

    private static int calcHp(int tier)    { return 180 + (tier - 1) * 540; }
    private static int calcAtk(int tier)   { return  30 + (tier - 1) * 2;  }
    private static int calcDef(int tier)   { return   5 + (tier - 1) * 3;   }
    private static int calcSpeed(int tier) { return  900 - (tier - 1) * 40; }

    // ── Passive ───────────────────────────────────────────────────────────────
    @Override
    public Passive getPassive() {
        return new Passive() {
            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.ON_TAKE_DAMAGE);
            }

            @Override public String getName()        { return "Composite Armor"; }
            @Override public String getDescription() { return (int)(DAMAGE_REDUCE * 100) + "% reduction on all incoming damage"; }
            @Override public int    getIntervalMs()  { return 0; }

            @Override
            public void trigger(PassiveContext ctx) {
                if (ctx.incomingDamage <= 0) return;
                // Reduce by 10%, floor at 1 so a hit always deals at least 1 damage
                int reduced = Math.max(1, (int)(ctx.incomingDamage * (1.0 - DAMAGE_REDUCE)));
                ctx.incomingDamage = reduced;
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