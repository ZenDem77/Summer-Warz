package Combat;

import Entities.Entity;

public interface Passive {
    int getIntervalMs();
    String getName();
    String getDescription();
    void trigger(Entity owner, Battle battle);
}
