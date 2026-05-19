package Entities.Enemies;

import Combat.IBattle;
import Entities.Entity;
import Entities.Enemy;
import Entities.Passive;
import Entities.PassiveContext;

public class Phainon extends Enemy {

    private static final int PASSIVE_HEAL_AMOUNT = 20;
    private static final int PASSIVE_INTERVAL_MS = 7000;

    private static final String[] TAUNTS = {
            "New sun, tear the sky!",
            "Quench the flames with blood!",
            "For tomorrow!",
            "All stars burn to ash."
    };

    public Phainon() {
        super("Phainon", 300, 15, 3, 1200, 0.05, 0.50, "Fire Ronin", 15, 30);
    }

    @Override
    public String getTaunt() {
        return TAUNTS[(int)(Math.random() * TAUNTS.length)];
    }

    @Override
    public Passive getPassive() {
        return new Passive() {
            @Override public String getName()        { return "Ember Mend"; }
            @Override public String getDescription() { return "Recover " + PASSIVE_HEAL_AMOUNT + " HP every 7s"; }
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

    @Override
    public String toString() { return "[" + rank + "] " + super.toString(); }
}