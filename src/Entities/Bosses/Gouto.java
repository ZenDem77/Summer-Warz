package Entities.Bosses;

import Entities.Enemy;
import Entities.Entity;
import Entities.PassiveHandler.*;

import java.util.EnumSet;
import java.util.Set;

// ─── BOSS 8 ───────────────────────────────────────────────────────────────────

public class Gouto extends Enemy implements Shielded {

    private static final int    PASSIVE_SHIELD_AMOUNT = 1000;
    private static final int    PASSIVE_DEF_GAIN      = 5;

    private int     shieldHp   = 0;
    private boolean triggered  = false;   // ensures shield granted once per battle
    private int     stackedDef = 0;       // bonus DEF accumulated from passive

    public Gouto() {
        super("Gouto", 15700, 50, 25, 500, 0.95, 2.25);
    }

    // ── Shielded interface ────────────────────────────────────────────────────

    @Override
    public int getShieldHp() { return shieldHp; }

    // ── Defense override ──────────────────────────────────────────────────────

    @Override
    public int getDefense() {
        return super.getDefense() + stackedDef;
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @Override
    public void reset() {
        super.reset();
        shieldHp   = 0;
        triggered  = false;
        stackedDef = 0;
    }

    // ── Passive ───────────────────────────────────────────────────────────────

    @Override
    public Passive getPassive() {
        return new Passive() {

            @Override public String getName() { return "Siege Armor"; }
            @Override public String getDescription() {
                return "Gain " + PASSIVE_SHIELD_AMOUNT + " shield at start of battle. "
                        + "Gains +" + PASSIVE_DEF_GAIN + " DEF on every normal damage hit.";
            }
            @Override public int getIntervalMs() { return 0; }

            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.BATTLE_START,
                        PassiveEvent.BATTLE_END,
                        PassiveEvent.ON_TAKE_DAMAGE);
            }

            // ── BATTLE_START: grant shield once ───────────────────────────────
            @Override
            public void onBattleStart(Entity owner, Combat.IBattle battle) {
                if (triggered) return;
                triggered = true;
                shieldHp  = PASSIVE_SHIELD_AMOUNT;
                battle.notifyShield(owner, getName(), PASSIVE_SHIELD_AMOUNT);
            }

            // ── BATTLE_END: reset state ────────────────────────────────────────
            @Override
            public void onBattleEnd(Entity owner, Combat.IBattle battle) {
                // reset() handles shieldHp/triggered/stackedDef cleanup
            }

            // ── ON_TAKE_DAMAGE: shield absorption + DEF gain ──────────────────
            // Fires only for normal damage (normal attacks + Zenzenkoi special).
            // True damage uses applyTrueDamageRaw which calls onTrueDamageReceived
            // instead — so DEF gain is correctly skipped for true-damage hits.
            @Override
            public void trigger(PassiveContext ctx) {
                if (ctx.event != PassiveEvent.ON_TAKE_DAMAGE) return;
                if (ctx.incomingDamage <= 0) return;

                // ── Shield absorption ─────────────────────────────────────────
                if (shieldHp > 0) {
                    if (ctx.incomingDamage <= shieldHp) {
                        shieldHp -= ctx.incomingDamage;
                        ctx.incomingDamage = 0;
                    } else {
                        ctx.incomingDamage -= shieldHp;
                        shieldHp = 0;
                    }
                }

                // ── DEF gain — triggers on every normal hit, even if fully absorbed
                stackedDef += PASSIVE_DEF_GAIN;
                ctx.battle.notifyPassive(ctx.owner, ctx.owner, getName(),
                        "DEF +" + PASSIVE_DEF_GAIN + " (now " + ctx.owner.getDefense() + " DEF)",
                        0, true);
            }
        };
    }
}