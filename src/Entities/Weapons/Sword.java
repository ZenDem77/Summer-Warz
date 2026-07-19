package Entities.Weapons;

import Entities.StatType;

public class Sword extends Weapon {

    public Sword() {
        super(
                "Sword",         // display name
                16,                    // base weapon ATK (level +0)
                8,                     // ATK gained per level
                StatType.ATK_PERCENT,  // secondary stat type
                0.11,                  // base secondary value 11% ATK
                0.0825                 // per milestone +8.25%
        );
    }
}