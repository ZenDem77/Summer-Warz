package Entities.Weapons.Hp;

import Entities.StatType;
import Entities.Weapons.Weapon;

public class Catalyst extends Weapon {

    public Catalyst() {
        super(
                "Catalyst",
                16,                // base ATK
                8,                        // ATK per level
                StatType.HP_PERCENT,      // secondary stat type
                0.08,                     // base HP% (8%)
                0.06                      // per milestone (+6%)
        );
    }
}