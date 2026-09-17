package Entities.Weapons.CritRate;

import Entities.StatType;
import Entities.Weapons.Weapon;

public class Gauntlet extends Weapon {

    public Gauntlet() {
        super(
                "Gauntlet",
                16,                       // base ATK
                8,                        // ATK per level
                StatType.CRIT_RATE,       // secondary stat type
                0.06,                     // base crit rate (6%)
                0.045                     // per milestone (+4.5%)
        );
    }
}