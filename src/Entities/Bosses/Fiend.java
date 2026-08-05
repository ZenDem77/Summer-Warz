package Entities.Bosses;

import Entities.Enemy;
import Entities.PassiveHandler.*;
import Entities.Sprites.*;

// ─── BOSS 1 ───────────────────────────────────────────────────────────────────

public class Fiend extends Enemy {

    // ── Passive constants ─────────────────────────────────────────────────────
    private static final int PASSIVE_HEAL_AMOUNT = 100;
    private static final int PASSIVE_INTERVAL_MS = 3000;

    private static String msToSec(int ms) {
        double s = ms / 1000.0;
        return (s == (int) s ? String.valueOf((int) s) : String.valueOf(s)) + "s";
    }

    public Fiend() {
        super("Fiend", 500, 15, 3, 900, 0.90, 1.80);
    }

    @Override
    public Passive getPassive() {
        return new Passive() {
            @Override public String getName()        { return "Ember Mend"; }
            @Override public String getDescription() { return "Recover " + PASSIVE_HEAL_AMOUNT + " HP every " + msToSec(PASSIVE_INTERVAL_MS); }
            @Override public int    getIntervalMs()  { return PASSIVE_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                int before = ctx.owner.getCurrentHp();
                ctx.owner.heal(PASSIVE_HEAL_AMOUNT);
                int healed = ctx.owner.getCurrentHp() - before;
                ctx.battle.notifyPassive(ctx.owner, ctx.owner, getName(),
                        "+" + healed + " HP restored", healed, true);
            }
        };
    }

    // ── Sprite ────────────────────────────────────────────────────────────────
    @Override
    public SpriteSet getSpriteSet() {
        return new SpriteSet(
                SpritePaths.FIEND_IDLE,
                SpritePaths.FIEND_RUN,
                SpritePaths.FIEND_ATTACK,
                SpritePaths.FIEND_DEAD
        );
    }
}