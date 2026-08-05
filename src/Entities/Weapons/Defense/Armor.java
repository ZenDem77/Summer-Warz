package Entities.Weapons.Defense;

import Entities.StatType;
import Entities.Weapons.Weapon;

public class Armor extends Weapon {

    public Armor() {
        super(
                "Armor",
                16,                        // base ATK
                8,                        // ATK per level
                StatType.DEF_PERCENT,     // secondary stat type
                0.11,                     // base DEF% (11%)
                0.0825                     // per milestone (+8.25%)
        );
    }
}