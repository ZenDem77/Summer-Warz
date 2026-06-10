package Entities.PassiveHandler;

import Combat.IBattle;
import Entities.Entity;

public class PassiveContext {
    public final Entity owner;
    public final Entity target;
    public final IBattle battle;
    public final PassiveEvent event;
    public int     incomingDamage;
    public final boolean isCrit;

    public PassiveContext(Entity owner, Entity target, IBattle battle,
                          PassiveEvent event, int incomingDamage, boolean isCrit) {
        this.owner          = owner;
        this.target         = target;
        this.battle         = battle;
        this.event          = event;
        this.incomingDamage = incomingDamage;
        this.isCrit         = isCrit;
    }
}