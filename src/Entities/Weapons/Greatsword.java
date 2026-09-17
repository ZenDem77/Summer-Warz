package Entities.Weapons;

import Entities.StatType;

public class Greatsword extends Weapon {

    public Greatsword() {
        super(
                "Greatsword",
                16,                       // base ATK
                8,                        // ATK per level
                StatType.CRIT_DAMAGE,     // secondary stat type
                0.12,                     // base crit damage (12%)
                0.09                      // per milestone (+9%)
        );
    }
}