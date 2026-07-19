package Entities.Weapons;

import Entities.StatType;

public class Staff extends Weapon {

    public Staff() {
        super(
                "Staff",
                8,                // base ATK
                4,                        // ATK per level
                StatType.HP_PERCENT,      // secondary stat type
                0.16,                     // base HP% (16%)
                0.12                      // per milestone (+12%)
        );
    }
}