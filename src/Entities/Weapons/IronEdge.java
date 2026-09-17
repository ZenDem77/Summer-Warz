package Entities.Weapons;

import Entities.StatType;

public class IronEdge extends Weapon {

    public IronEdge() {
        super(
                "Iron Edge",          // display name           — change when finalised
                8,                   // base weapon ATK (level +0)
                4,                    // ATK gained per level
                StatType.CRIT_RATE,   // secondary stat type
                0.12,                 // base secondary value (level +0) — 12% crit rate
                0.09                  // secondary gained per milestone (every 5 levels) — +9%
        );
    }
}