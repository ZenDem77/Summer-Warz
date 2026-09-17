package Entities.Weapons.CritDmg;

import Entities.StatType;
import Entities.Weapons.Weapon;

public class Katana extends Weapon {

    public Katana() {
        super(
                "Katana",
                8,                        // base ATK
                4,                        // ATK per level
                StatType.CRIT_DAMAGE,     // secondary stat type
                0.24,                     // base crit damage (24%)
                0.18                      // per milestone (+18%)
        );
    }
}