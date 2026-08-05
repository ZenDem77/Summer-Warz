package Entities.Bosses;

import Entities.Enemy;
import Entities.Entity;
import Entities.PassiveHandler.Passive;
import Entities.PassiveHandler.PassiveContext;
import Entities.PassiveHandler.PassiveEvent;

import java.util.EnumSet;
import java.util.Set;

// ─── BOSS 10 ──────────────────────────────────────────────────────────────────

public class DarkPriest extends Enemy {

    private static final int    CRIT_AMPLIFIER        = 2;      // ×2 damage on crits
    private static final double HEAL_TRIGGER_THRESHOLD = 0.10;  // 10% HP
    private static final int    HEAL_AMOUNT            = 300;   // per tick
    private static final int    HEAL_INTERVAL_MS       = 50;    // 0.05s between heals
    private static final int    HEAL_DURATION_MS       = 3000;  // 3s total
    private static final int    HEAL_TICKS             = HEAL_DURATION_MS / HEAL_INTERVAL_MS; // 60

    private boolean healTriggered = false;

    public DarkPriest() {
        super("Dark Priest", 20000, 60, 15, 500, 0.95, 2.25);
    }

    @Override
    public void reset() {
        super.reset();
        healTriggered = false;
    }

    @Override
    public Passive getPassive() {
        return new Passive() {

            @Override public String getName() { return "Dark Ritual"; }
            @Override public String getDescription() {
                return "Crit hits deal ×" + CRIT_AMPLIFIER + " damage. "
                        + "At " + (int)(HEAL_TRIGGER_THRESHOLD * 100) + "% HP: "
                        + "heal " + HEAL_AMOUNT + " HP every " + HEAL_INTERVAL_MS + "ms "
                        + "for " + (HEAL_DURATION_MS / 1000) + "s.";
            }
            @Override public int getIntervalMs() { return 0; }

            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.ON_TAKE_DAMAGE,
                        PassiveEvent.BATTLE_END);
            }

            @Override
            public void onBattleEnd(Entity owner, Combat.IBattle battle) {
                healTriggered = false;
            }

            @Override
            public void trigger(PassiveContext ctx) {
                if (ctx.event == PassiveEvent.BATTLE_END) return;

                // ── Effect 1: Crit Amplification ─────────────────────────────
                // Doubles damage on any crit — normal attack crits AND special
                // attack crits (e.g. Zenzenkoi P3) that route through
                // ON_TAKE_DAMAGE with isCrit=true.
                if (ctx.isCrit && ctx.incomingDamage > 0) {
                    ctx.incomingDamage *= CRIT_AMPLIFIER;
                }

                // ── Effect 2: Heal trigger check ──────────────────────────────
                // Check if this hit will drop HP to ≤10%. We compute post-hit
                // HP before damage lands, same pattern as Shogun's phase 2.
                if (!healTriggered) {
                    int hpAfterHit = ctx.owner.getCurrentHp() - ctx.incomingDamage;
                    double pctAfterHit = (double) hpAfterHit / ctx.owner.getMaxHp();

                    if (pctAfterHit <= HEAL_TRIGGER_THRESHOLD) {
                        healTriggered = true;
                        startHealTimer(ctx.owner, ctx.battle);
                    }
                }
            }
        };
    }

    // ── Heal timer ────────────────────────────────────────────────────────────

    private void startHealTimer(Entity owner, Combat.IBattle battle) {
        // Track remaining ticks in a 1-element array so the lambda can mutate it
        int[] remaining = { HEAL_TICKS };

        javax.swing.Timer healTimer = new javax.swing.Timer(HEAL_INTERVAL_MS, e -> {
            javax.swing.Timer self = (javax.swing.Timer) e.getSource();

            if (!owner.isAlive() || remaining[0] <= 0) {
                battle.unregisterPausableTimer(self);
                self.stop();
                return;
            }

            int before = owner.getCurrentHp();
            owner.heal(HEAL_AMOUNT);
            int healed = owner.getCurrentHp() - before;

            if (healed > 0) {
                battle.notifyPassive(owner, owner, "Dark Ritual",
                        "+" + healed + " HP (Dark Regeneration, "
                                + remaining[0] + " ticks left)", healed, true);
            }

            remaining[0]--;
            if (remaining[0] <= 0) {
                battle.unregisterPausableTimer(self);
                self.stop();
            }
        });

        healTimer.setRepeats(true);
        healTimer.start();
        battle.registerPausableTimer(healTimer);
    }
}