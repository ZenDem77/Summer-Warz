package Entities.Enemies;

import Entities.Entity;
import Entities.Enemy;
import Combat.NormalBattle.Battle;
import Entities.Passive;

public class Phainon extends Enemy {

    private static final int PASSIVE_HEAL_AMOUNT  = 20;
    private static final int PASSIVE_INTERVAL_MS  = 7000;

    private static final String[] TAUNTS = {
            "New sun, tear the sky!",
            "Quench the flames with blood!",
            "For tomorrow!",
            "All stars burn to ash."
    };

    public Phainon() {
        super("Phainon", 300, 15, 3, 1200, 0.05, 1.50, "Fire Ronin", 15, 30);
    }

    @Override
    public String getTaunt() {
        return TAUNTS[(int)(Math.random() * TAUNTS.length)];
    }

    @Override
    public Passive getPassive() {
        return new Passive() {
            @Override public int    getIntervalMs() { return PASSIVE_INTERVAL_MS; }
            @Override public String getName()       { return "Ember Mend"; }
            @Override public String getDescription() { return "Recover " + PASSIVE_HEAL_AMOUNT + " HP every 7s"; }

            @Override
            public void trigger(Entity owner, Battle battle) {
                int before = owner.getCurrentHp();
                owner.heal(PASSIVE_HEAL_AMOUNT);
                int healed = owner.getCurrentHp() - before;
                battle.notifyPassive(owner, owner, getName(),
                        "+" + healed + " HP restored", healed, true);
            }
        };
    }

    @Override
    public String toString() { return "[" + rank + "] " + super.toString(); }
}