package Entities.Bosses;

import Entities.Enemy;
import Entities.PassiveHandler.Passive;
import Entities.PassiveHandler.PassiveContext;
import Entities.PassiveHandler.PassiveEvent;
import Entities.PassiveHandler.Shielded;

import java.util.EnumSet;
import java.util.Set;

public class Lynx extends Enemy implements Shielded {

    private static final int    SHIELD_AMOUNT       = 2000;
    private static final int    BASE_ATTACK_SPEED   = 1200;
    private static final int    ENRAGED_ATTACK_SPEED = 400;

    private int     shieldHp = 0;
    private boolean enraged  = false;   // true once the shield has broken

    public Lynx() {
        super("Lynx", 2200, 17, 10, BASE_ATTACK_SPEED, 0.90, 1.75);
    }

    // ── Shielded interface ────────────────────────────────────────────────────

    @Override
    public int getShieldHp() { return shieldHp; }

    // ── Reset (clears shield + attack speed on battle start / retry) ─────────

    @Override
    public void reset() {
        super.reset();
        shieldHp = 0;
        enraged  = false;
        setAttackSpeed(BASE_ATTACK_SPEED);
    }

    // ── Passive ───────────────────────────────────────────────────────────────
    @Override
    public Passive getPassive() {
        return new Passive() {

            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.BATTLE_START, PassiveEvent.ON_TAKE_DAMAGE);
            }

            @Override public String getName()        { return "Iron Vigil"; }
            @Override public String getDescription() {
                return "Gains a " + SHIELD_AMOUNT + " HP shield at the start of battle. "
                        + "Once the shield breaks, attack speed increases to "
                        + (ENRAGED_ATTACK_SPEED / 1000.0) + "s.";
            }
            @Override public int getIntervalMs() { return 0; }

            @Override
            public void onBattleStart(Entities.Entity owner, Combat.IBattle battle) {
                shieldHp = SHIELD_AMOUNT;
                battle.notifyShield(owner, getName(), SHIELD_AMOUNT);
            }

            @Override
            public void trigger(PassiveContext ctx) {
                if (ctx.event != PassiveEvent.ON_TAKE_DAMAGE) return;
                if (shieldHp <= 0 || ctx.incomingDamage <= 0) return;

                boolean willBreak = ctx.incomingDamage >= shieldHp;

                if (ctx.incomingDamage <= shieldHp) {
                    // Shield absorbs all — HP is untouched
                    shieldHp -= ctx.incomingDamage;
                    ctx.incomingDamage = 0;
                } else {
                    // Shield absorbs partially — remainder hits HP
                    ctx.incomingDamage -= shieldHp;
                    shieldHp = 0;
                }

                if (willBreak && !enraged) {
                    enraged = true;
                    ctx.owner.setAttackSpeed(ENRAGED_ATTACK_SPEED);
                    ctx.battle.notifyPassive(ctx.owner, ctx.owner, getName(),
                            "Shield broken! Attack speed increased to "
                                    + (ENRAGED_ATTACK_SPEED / 1000.0) + "s",
                            0, false);
                }
            }
        };
    }

    @Override
    public String toString() { return super.toString(); }

    // In Lynx.java or a future big boss
    //@Override public double getSizeScale() { return 2.5; }
}