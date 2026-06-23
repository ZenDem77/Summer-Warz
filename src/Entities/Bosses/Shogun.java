package Entities.Bosses;

import Entities.Enemy;
import Entities.Entity;
import Entities.PassiveHandler.Passive;
import Entities.PassiveHandler.PassiveContext;
import Entities.PassiveHandler.PassiveEvent;
import Entities.PassiveHandler.Shielded;

import java.util.EnumSet;
import java.util.Set;

public class Shogun extends Enemy implements Shielded {

    private static final int    BASE_ATTACK_SPEED    = 400;
    private static final int    PHASE_2_ATTACK_SPEED = 700;
    private static final double PHASE_2_HP_THRESHOLD = 0.50;
    private static final int    SHIELD_AMOUNT        = 3500;

    private int     shieldHp   = 0;
    private boolean triggered  = false;   // true once the 50% threshold fired

    // ── Constructor ───────────────────────────────────────────────────────────

    public Shogun() {
        super("Shogun", 7500, 20, 12, BASE_ATTACK_SPEED, 0.90, 2.00);
    }

    // ── Shielded interface ────────────────────────────────────────────────────

    @Override
    public int getShieldHp() { return shieldHp; }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @Override
    public void reset() {
        super.reset();
        shieldHp  = 0;
        triggered = false;
        setAttackSpeed(BASE_ATTACK_SPEED);
    }

    // ── Passive ───────────────────────────────────────────────────────────────

    @Override
    public Passive getPassive() {
        return new Passive() {

            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.ON_TAKE_DAMAGE);
            }

            @Override public String getName() { return "Iron Bastion"; }
            @Override public String getDescription() {
                return "At 50% HP: gain " + SHIELD_AMOUNT + " shield — damage absorbed heals HP. "
                        + "Attack speed slows to " + (PHASE_2_ATTACK_SPEED / 1000.0) + "s. Once per battle.";
            }
            @Override public int getIntervalMs() { return 0; }

            @Override
            public void trigger(PassiveContext ctx) {
                if (ctx.incomingDamage <= 0) return;

                // ── Phase 2 trigger check ──────────────────────────────────────
                // Run BEFORE absorbing damage so we can check if this hit pushes
                // HP to the threshold. We compute what HP will be after the hit
                // lands on actual HP (not shieldHp — at trigger time shield is 0).
                if (!triggered) {
                    int hpAfterHit = ctx.owner.getCurrentHp() - ctx.incomingDamage;
                    double hpPctAfterHit = (double) hpAfterHit / ctx.owner.getMaxHp();

                    if (hpPctAfterHit <= PHASE_2_HP_THRESHOLD) {
                        triggered = true;
                        shieldHp = SHIELD_AMOUNT;
                        ctx.owner.setAttackSpeed(PHASE_2_ATTACK_SPEED);
                        ctx.battle.notifyShield(ctx.owner, getName(), SHIELD_AMOUNT);
                        ctx.battle.notifyPassive(ctx.owner, ctx.owner, getName(),
                                "Phase 2! Shield granted, attack speed reduced",
                                0, false);
                    }
                }

                // ── Shield absorption + HP conversion ─────────────────────────
                if (shieldHp <= 0 || ctx.incomingDamage <= 0) return;

                int absorbed = Math.min(ctx.incomingDamage, shieldHp);
                shieldHp -= absorbed;
                ctx.incomingDamage -= absorbed;

                // The absorbed amount heals Shogun's HP directly
                int before = ctx.owner.getCurrentHp();
                ctx.owner.heal(absorbed);
                int healed = ctx.owner.getCurrentHp() - before;
                if (healed > 0) {
                    ctx.battle.notifyPassive(ctx.owner, ctx.owner, getName(),
                            "+" + healed + " HP restored from shield absorption",
                            healed, true);
                }
            }
        };
    }

    @Override
    public String toString() { return super.toString(); }
}