package Entities.Enemies;

import Combat.IBattle;
import Entities.Enemy;
import Entities.Entity;
import Entities.Passive;
import Entities.PassiveContext;
import Entities.PassiveEvent;
import Entities.Shielded;

import java.util.EnumSet;
import java.util.Set;

public class IndestructibleSubject extends Enemy implements Shielded {

    private static final double CRIT_RATE       = 0.75;
    private static final double CRIT_DAMAGE     = 1.80;
    private static final int    SHIELD_INTERVAL = 2000;  // ms between shield grants

    private final int tier;
    private       int shieldHp = 0;

    // ── Constructor ───────────────────────────────────────────────────────────

    public IndestructibleSubject(int tier) {
        super("Indestructible Subject",
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
                    "IndestructibleSubject tier must be 1–10, got: " + tier);
    }

    private static int calcHp(int tier)    { return tier * 200; }
    private static int calcAtk(int tier)   { return  28 + (tier - 1) * 6;  }
    private static int calcDef(int tier)   { return   8 + (tier - 1) * 2;  }
    private static int calcSpeed(int tier) { return 1000 - (tier - 1) * 50; }

    // ── Shield formula ────────────────────────────────────────────────────────
    private int shieldPerStack() { return 20 + tier * 8; }

    // ── Shielded interface ────────────────────────────────────────────────────
    @Override
    public int getShieldHp() { return shieldHp; }

    // ── Reset (clears shield on battle start / retry) ─────────────────────────
    @Override
    public void reset() {
        super.reset();
        shieldHp = 0;
    }

    // ── Passive ───────────────────────────────────────────────────────────────
    @Override
    public Passive getPassive() {
        return new Passive() {

            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.TICK, PassiveEvent.ON_TAKE_DAMAGE);
            }

            @Override public String getName()        { return "Iron Shield"; }
            @Override public String getDescription() { return "Gains " + shieldPerStack() + " shield every 2s (stacks)"; }
            @Override public int    getIntervalMs()  { return SHIELD_INTERVAL; }

            @Override
            public void trigger(PassiveContext ctx) {
                if (!ctx.owner.isAlive()) return;

                switch (ctx.event) {

                    case TICK -> {
                        // ── Grant shield stack ────────────────────────────────
                        int gain = shieldPerStack();
                        shieldHp += gain;
                        ctx.battle.notifyShield(ctx.owner, getName(), gain);
                    }

                    case ON_TAKE_DAMAGE -> {
                        // ── Absorb incoming damage into shield ────────────────
                        if (shieldHp <= 0 || ctx.incomingDamage <= 0) return;

                        if (ctx.incomingDamage <= shieldHp) {
                            // Shield absorbs all — HP is untouched
                            shieldHp -= ctx.incomingDamage;
                            ctx.incomingDamage = 0;
                        } else {
                            // Shield absorbs partially — remainder hits HP
                            ctx.incomingDamage -= shieldHp;
                            shieldHp = 0;
                        }
                    }

                    default -> { /* BATTLE_START / BATTLE_END — nothing to do */ }
                }
            }
        };
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public int getTier() { return tier; }

    @Override
    public String toString() { return "[Tier " + tier + "] " + super.toString(); }
}