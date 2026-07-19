package Entities.Weapons;

import Entities.StatType;

public class Saber extends Weapon {

    public Saber() {
        super(
                "Saber",          // display name           — change when finalised
                8,                   // base weapon ATK (level +0)
                4,                    // ATK gained per level
                StatType.CRIT_RATE,   // secondary stat type
                0.12,                 // base secondary value 12% crit rate
                0.09                  // per milestone +9%
        );
    }
}