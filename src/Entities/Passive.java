package Entities;

import Combat.NormalBattle.Battle;

public interface Passive {
    int getIntervalMs();
    String getName();
    String getDescription();
    void trigger(Entity owner, Battle battle);
}
