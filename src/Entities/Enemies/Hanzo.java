package Entities.Enemies;

import Entities.Enemy;
import Entities.Passive;
import Entities.PassiveContext;
import Entities.PassiveEvent;
import java.util.EnumSet;
import java.util.Set;

public class Hanzo extends Enemy {

    private static final double CRIT_REDUCTION = 0.50;   // 50% crit damage reduction

    public Hanzo() {
        super("Hanzo", 700, 13, 7, 500, 0.80, 2.00);
    }

    @Override
    public Passive getPassive() {
        return new Passive() {
            @Override public String getName()        { return "Iron Resolve"; }
            @Override public String getDescription() { return "Reduces incoming crit damage by " + (int)(CRIT_REDUCTION * 100) + "%"; }
            @Override public int    getIntervalMs()  { return 0; }

            @Override
            public java.util.Set<PassiveEvent> respondsTo() {
                return java.util.EnumSet.of(PassiveEvent.ON_TAKE_DAMAGE);
            }

            @Override
            public void trigger(PassiveContext ctx) {
                if (!ctx.isCrit) return;   // only intercept crits
                int reduced = (int)(ctx.incomingDamage * CRIT_REDUCTION);
                ctx.incomingDamage -= reduced;
                ctx.battle.notifyPassive(ctx.owner, ctx.owner, getName(),
                        "crit reduced by " + reduced + " dmg", 0, false);
            }
        };
    }
}
