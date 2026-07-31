package Entities.Bosses;

import Entities.Enemy;
import Entities.Entity;
import Entities.PassiveHandler.Passive;
import Entities.PassiveHandler.PassiveContext;
import Entities.PassiveHandler.PassiveEvent;

import java.util.EnumSet;

// ─── BOSS 6 ───────────────────────────────────────────────────────────────────

public class Stain extends Enemy {

    private static final double TRUE_DMG_REDUCTION   = 0.50;
    private static final int    RETALIATION_DAMAGE   = 10;

    public Stain() {
        super("Stain", 12500, 34, 10, 500, 0.95, 2.25);
    }

    // ── True damage hooks ─────────────────────────────────────────────────────

    @Override
    public double getTrueDamageReduction() { return TRUE_DMG_REDUCTION; }

    @Override
    public void onTrueDamageReceived(Entity attacker, int amount, Combat.IBattle battle) {
        if (!attacker.isAlive()) return;

        // Retaliation is also true damage — scale by getDamageMultiplier()
        // so it's correctly reduced in Spar mode (CharacterBattle).
        int retaliationDmg = Math.max(1,
                (int)(RETALIATION_DAMAGE * battle.getDamageMultiplier()));
        int actual = Math.min(retaliationDmg, attacker.getCurrentHp());
        attacker.takeDamage(retaliationDmg);

        battle.notifyPassive(this, attacker, "True Reversal",
                actual + " retaliation true dmg", actual, false);
        battle.checkEndPublic();
    }

    // ── Passive (display) ─────────────────────────────────────────────────────

    @Override
    public Passive getPassive() {
        return new Passive() {
            @Override public String getName()        { return "True Reversal"; }
            @Override public String getDescription() {
                return "Reduces all incoming true damage by "
                        + (int)(TRUE_DMG_REDUCTION * 100) + "%. "
                        + "Retaliates with " + RETALIATION_DAMAGE
                        + " true damage on every true-damage hit.";
            }
            @Override public int getIntervalMs() { return 0; }

            @Override
            public java.util.Set<PassiveEvent> respondsTo() {
                return EnumSet.noneOf(PassiveEvent.class);
            }

            // No trigger — both effects are handled by Entity hooks called
            // inside IBattle.applyTrueDamageRaw() on every true-damage hit.
            @Override
            public void trigger(PassiveContext ctx) {}
        };
    }
}