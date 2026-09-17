package Entities.Bosses;

import Entities.Enemy;
import Entities.PassiveHandler.Passive;
import Entities.PassiveHandler.PassiveContext;
import Entities.PassiveHandler.PassiveEvent;

import java.util.EnumSet;
import java.util.Set;

// ─── BOSS 7 ───────────────────────────────────────────────────────────────────

public class Groon extends Enemy {

    private static final int PASSIVE_DEF_GAIN    = 10;
    private static final int PASSIVE_INTERVAL_MS = 1000;

    private int stackedDef = 0;

    public Groon() {
        super("Groon", 14500, 50, 20, 700, 0.95, 2.25);
    }

    // ── Defense override ──────────────────────────────────────────────────────
    @Override
    public int getDefense() {
        return super.getDefense() + stackedDef;
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @Override
    public void reset() {
        super.reset();
        stackedDef = 0;
    }

    // ── Passive ───────────────────────────────────────────────────────────────

    @Override
    public Passive getPassive() {
        return new Passive() {

            @Override public String getName()        { return "Iron Growth"; }
            @Override public String getDescription() {
                return "Gains +" + PASSIVE_DEF_GAIN + " DEF every "
                        + (PASSIVE_INTERVAL_MS / 1000.0) + "s. ";
            }
            @Override public int getIntervalMs() { return PASSIVE_INTERVAL_MS; }

            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.TICK);
            }

            @Override
            public void trigger(PassiveContext ctx) {
                stackedDef += PASSIVE_DEF_GAIN;
                ctx.battle.notifyPassive(ctx.owner, ctx.owner, getName(),
                        "DEF +" + PASSIVE_DEF_GAIN + " (now " + ctx.owner.getDefense() + " DEF)",
                        0, true);
            }
        };
    }
}