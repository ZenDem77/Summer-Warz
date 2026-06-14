package Entities.Characters;

import Combat.DamageResult;
import Entities.Entity;
import Entities.Character;
import Entities.PassiveHandler.*;

import java.util.EnumSet;
import java.util.Set;

public class Zayir extends Character {

    // ── Passive 1 constants ───────────────────────────────────────────────────
    private static final double PASSIVE_1_CRIT_RATE_PENALTY = 2.00;   // -200%
    private static final int    PASSIVE_1_FLAT_DAMAGE       = 10;
    private static final double PASSIVE_1_ATK_PERCENT       = 0.70;   // 70% of total ATK
    private static final int    PASSIVE_1_INTERVAL_MS       = 400;

    // ── Passive 2 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_2_ATK_BONUS    = 100;
    private static final int PASSIVE_2_DURATION_MS  = 2400;

    // ── Passive 3 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_3_ATK_BONUS = 50;

    public Zayir() {
        super("Zayir", 250, 50, 3, 4000, 0.05, 0.50, 1);
    }

    @Override
    public Passive[] getPassives() {
        return new Passive[]{ passive1(), passive2(), passive3() };
    }

    private static String msToSec(int ms) {
        double s = ms / 1000.0;
        return (s == (int) s ? String.valueOf((int) s) : String.valueOf(s)) + "s";
    }

    private Passive passive1() {
        return new Passive() {
            @Override public String getName()        { return "Passive Strike"; }
            @Override public String getDescription() {
                return "Crit Rate -" + (int)(PASSIVE_1_CRIT_RATE_PENALTY * 100) + "%. "
                        + "Deal " + PASSIVE_1_FLAT_DAMAGE + " + " + (int)(PASSIVE_1_ATK_PERCENT * 100)
                        + "% Total ATK True Damage every " + msToSec(PASSIVE_1_INTERVAL_MS);
            }
            @Override public int getIntervalMs() { return PASSIVE_1_INTERVAL_MS; }

            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.TICK, PassiveEvent.BATTLE_START, PassiveEvent.BATTLE_END);
            }

            @Override
            public void onBattleStart(Entity owner, Combat.IBattle battle) {
                owner.addCritRate(-PASSIVE_1_CRIT_RATE_PENALTY);
            }

            @Override
            public void onBattleEnd(Entity owner, Combat.IBattle battle) {
                owner.addCritRate(PASSIVE_1_CRIT_RATE_PENALTY);
            }

            @Override
            public void trigger(PassiveContext ctx) {
                if (ctx.event != PassiveEvent.TICK) return;

                Entity target = ctx.battle.getOpponent(ctx.owner);
                int dmg = PASSIVE_1_FLAT_DAMAGE + (int) (PASSIVE_1_ATK_PERCENT * ctx.owner.getEffectiveAtk());
                DamageResult result = ctx.owner.calculateTrueDamage(dmg);
                if (result.isMiss) return;
                int actual = Math.min(result.amount, target.getCurrentHp());
                target.takeDamage(result.amount);
                ctx.battle.notifyPassive(ctx.owner, target, getName(),
                        actual + " true damage", actual, false);
                ctx.battle.checkEndPublic();
            }
        };
    }

    private Passive passive2() {
        return new Passive() {
            @Override public String getName()        { return "Rage"; }
            @Override public String getDescription() {
                return "On entering battle, gain +" + PASSIVE_2_ATK_BONUS
                        + " ATK for the first " + msToSec(PASSIVE_2_DURATION_MS);
            }
            @Override public int getIntervalMs() { return 0; }

            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.BATTLE_START, PassiveEvent.BATTLE_END);
            }

            private boolean bonusActive = false;
            private javax.swing.Timer expireTimer = null;

            @Override
            public void onBattleStart(Entity owner, Combat.IBattle battle) {
                if (!(owner instanceof Character c)) return;

                c.addPassiveAtkBonus(PASSIVE_2_ATK_BONUS);
                bonusActive = true;

                expireTimer = new javax.swing.Timer(PASSIVE_2_DURATION_MS, e -> {
                    if (bonusActive) {
                        c.removePassiveAtkBonus(PASSIVE_2_ATK_BONUS);
                        bonusActive = false;
                    }
                    ((javax.swing.Timer) e.getSource()).stop();
                });
                expireTimer.setRepeats(false);
                expireTimer.start();
            }

            @Override
            public void onBattleEnd(Entity owner, Combat.IBattle battle) {
                if (!(owner instanceof Character c)) return;

                if (expireTimer != null) {
                    expireTimer.stop();
                    expireTimer = null;
                }
                if (bonusActive) {
                    c.removePassiveAtkBonus(PASSIVE_2_ATK_BONUS);
                    bonusActive = false;
                }
            }

            @Override
            public void trigger(PassiveContext ctx) { /* lifecycle-only passive */ }
        };
    }

    private Passive passive3() {
        return new Passive() {
            @Override public String getName()        { return "Empower"; }
            @Override public String getDescription() {
                return "Increase ATK by " + PASSIVE_3_ATK_BONUS;
            }
            @Override public int getIntervalMs() { return 0; }

            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.BATTLE_START, PassiveEvent.BATTLE_END);
            }

            @Override
            public void onBattleStart(Entity owner, Combat.IBattle battle) {
                if (owner instanceof Character c) c.addPassiveAtkBonus(PASSIVE_3_ATK_BONUS);
            }

            @Override
            public void onBattleEnd(Entity owner, Combat.IBattle battle) {
                if (owner instanceof Character c) c.removePassiveAtkBonus(PASSIVE_3_ATK_BONUS);
            }

            @Override
            public void trigger(PassiveContext ctx) { /* lifecycle-only passive */ }
        };
    }

    // ── Level up ──────────────────────────────────────────────────────────────
    @Override
    public void levelUp() {
        switch (checkLevel()) {
            case 1 -> { maxHp += 5;  attack += 2; }
            case 2 -> { maxHp += 7;  attack += 3; }
            case 3 -> { maxHp += 9;  attack += 4; }
            case 4 -> { maxHp += 13; attack += 5; defense += 1; }
        }
    }

    @Override
    public String toString() { return super.toString(); }
}