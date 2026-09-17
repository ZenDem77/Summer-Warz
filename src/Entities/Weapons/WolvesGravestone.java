package Entities.Weapons;

import Entities.StatType;

public class WolvesGravestone extends Weapon {

    public WolvesGravestone() {
        super(
                "Wolf's Gravestone",   // display name
                16,                    // base weapon ATK (level +0)
                8,                     // ATK gained per level
                StatType.ATK_PERCENT,  // secondary stat type
                0.11,                  // base secondary value (level +0) — 11% ATK
                0.0825                 // secondary gained per milestone (every 5 levels) — +8.25%
        );
    }
}