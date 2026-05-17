package Entities;

import Combat.Battle;
import Combat.Passive;

public class Zed extends Character {

    private static final int PASSIVE_BONUS_DAMAGE = 20;
    private static final int PASSIVE_INTERVAL_MS  = 5000;

    public Zed() {
        super("Zed", 120, 18, 5, 700, 1, "Shadow");
    }

    @Override
    public String getSpecialMoveName() { return "Shadow Strike"; }

    @Override
    public Passive getPassive() {
        return new Passive() {
            @Override public int    getIntervalMs() { return PASSIVE_INTERVAL_MS; }
            @Override public String getName()       { return "Shadow Surge"; }
            @Override public String getDescription() { return "Deal extra 20 Damage every 5 seconds"; }

            @Override
            public void trigger(Entity owner, Battle battle) {
                Entity target = battle.getEnemy();
                target.takeDamage(PASSIVE_BONUS_DAMAGE);
                battle.notifyPassive(owner, target, getName(),
                        PASSIVE_BONUS_DAMAGE + " bonus dmg (bypasses DEF)", false);
                battle.checkEndPublic();
            }
        };
    }

    @Override
    public String toString() { return "[" + clan + " Clan] " + super.toString(); }
}