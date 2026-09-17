package Entities.Bosses;

import Entities.Enemy;
import Entities.Entity;
import Entities.PassiveHandler.Passive;
import Entities.PassiveHandler.PassiveContext;
import Entities.PassiveHandler.PassiveEvent;

import java.util.EnumSet;
import java.util.Set;

// ─── BOSS 9 ───────────────────────────────────────────────────────────────────

public class Maki extends Enemy {

    // ── Passive constants ─────────────────────────────────────────────────────
    private static final double CRIT_REDUCTION       = 0.90;   // 90% reduction → keep 10%
    private static final int    RETALIATION_HITS     = 3;
    private static final int    RETALIATION_DAMAGE   = 40;
    private static final int    RETALIATION_DELAY_MS = 100;

    public Maki() {
        super("Maki", 18000, 0, 10, 700, 0.95, 2.20);
    }

    @Override
    public Passive getPassive() {
        return new Passive() {

            @Override public String getName() { return "Counter Edge"; }
            @Override public String getDescription() {
                return "Reduces crit damage taken by " + (int)(CRIT_REDUCTION * 100) + "%. "
                        + "Retaliates " + RETALIATION_HITS + "×" + RETALIATION_DAMAGE
                        + " dmg on every crit hit.";
            }
            @Override public int getIntervalMs() { return 0; }

            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.ON_TAKE_DAMAGE);
            }

            @Override
            public void trigger(PassiveContext ctx) {
                if (!ctx.isCrit) return;   // only intercept crit hits

                // ── 1. Reduce crit damage by 90% ────────────────────────────
                // Keep only 10% of the incoming damage (same pattern as Hanzo).
                int reduced = Math.max(1, (int)(ctx.incomingDamage * (1.0 - CRIT_REDUCTION)));
                ctx.incomingDamage = reduced;

                // ── 2. Retaliate 3×40 dmg with 50ms between each popup ──────
                // Each hit is a separate one-shot timer staggered by
                // RETALIATION_DELAY_MS so the damage numbers appear as a
                // visible combo rather than overlapping instantly.
                Entity attacker = ctx.battle.getOpponent(ctx.owner);
                Combat.IBattle battle = ctx.battle;

                for (int i = 0; i < RETALIATION_HITS; i++) {
                    final int hitNumber = i + 1;
                    final int delayMs   = RETALIATION_DELAY_MS * hitNumber;

                    javax.swing.Timer retaliationTimer = new javax.swing.Timer(delayMs, e -> {
                        javax.swing.Timer self = (javax.swing.Timer) e.getSource();
                        battle.unregisterPausableTimer(self);
                        self.stop();

                        if (!attacker.isAlive() || !ctx.owner.isAlive()) return;

                        int dmg    = Math.max(1, (int)(RETALIATION_DAMAGE * battle.getDamageMultiplier()));
                        int actual = Math.min(dmg, attacker.getCurrentHp());
                        attacker.takeDamage(dmg);

                        battle.notifyPassive(ctx.owner, attacker, getName(),
                                "(" + hitNumber + "/" + RETALIATION_HITS + ") "
                                        + actual + " retaliation dmg", actual, false);
                        battle.checkEndPublic();
                    });
                    retaliationTimer.setRepeats(false);
                    retaliationTimer.start();
                    battle.registerPausableTimer(retaliationTimer);
                }
            }
        };
    }
}