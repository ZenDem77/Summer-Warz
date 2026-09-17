package Entities.Weapons.Defense;

import Entities.StatType;
import Entities.Weapons.Weapon;

public class Shield extends Weapon {

    public Shield() {
        super(
                "Shield",
                8,                        // base ATK
                4,                        // ATK per level
                StatType.DEF_PERCENT,     // secondary stat type
                0.22,                     // base DEF% (22%)
                0.165                     // per milestone (+16.5%)
        );
    }
}