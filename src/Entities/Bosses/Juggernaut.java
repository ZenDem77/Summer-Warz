package Entities.Bosses;

import Entities.Enemy;
import Entities.PassiveHandler.Passive;
import Entities.PassiveHandler.PassiveContext;
import Entities.PassiveHandler.PassiveEvent;

import java.util.EnumSet;

// ─── BOSS 5 ───────────────────────────────────────────────────────────────────

public class Juggernaut extends Enemy {

    public Juggernaut() {
        super("Juggernaut", 10000, 27, 15, 400, 0.95, 2.20);
    }

    // ── True damage immunity ──────────────────────────────────────────────────

    @Override
    public boolean isTrueDamageImmune() { return true; }

    // ── Passive ───────────────────────────────────────────────────────────────

    @Override
    public Passive getPassive() {
        return new Passive() {
            @Override public String getName()        { return "True Immunity"; }
            @Override public String getDescription() {
                return "Immune to all true damage.";
            }
            @Override public int getIntervalMs()     { return 0; }

            @Override
            public java.util.Set<PassiveEvent> respondsTo() {
                return EnumSet.noneOf(PassiveEvent.class);
            }

            // No trigger logic — immunity is enforced by isTrueDamageImmune()
            // checked inside every true-damage passive before takeDamage() is called.
            @Override
            public void trigger(PassiveContext ctx) {}
        };
    }
}