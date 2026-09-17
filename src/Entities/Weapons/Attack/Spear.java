package Entities.Weapons.Attack;

import Entities.StatType;
import Entities.Weapons.Weapon;

public class Spear extends Weapon {

    public Spear() {
        super(
                "Spear",
                8,                        // base ATK
                4,                        // ATK per level
                StatType.ATK_PERCENT,     // secondary stat type
                0.22,                     // base ATK% (22%)
                0.165                     // per milestone (+16.5%)
        );
    }
}