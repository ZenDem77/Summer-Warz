package Entities;

import Combat.IBattle;

public class PassiveContext {
    public final Entity owner;
    public final Entity target;
    public final IBattle battle;
    public final PassiveEvent event;
    public int incomingDamage;

    public PassiveContext(Entity owner, Entity target, IBattle battle,
                          PassiveEvent event, int incomingDamage) {
        this.owner          = owner;
        this.target         = target;
        this.battle         = battle;
        this.event          = event;
        this.incomingDamage = incomingDamage;
    }
}
