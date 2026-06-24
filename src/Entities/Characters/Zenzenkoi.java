package Entities.Characters;

import Combat.DamageResult;
import Entities.Entity;
import Entities.Character;
import Entities.PassiveHandler.*;
import Entities.Sprites.*;

import java.util.EnumSet;
import java.util.Set;

public class Zenzenkoi extends Character {

    // ── Passive 1 constants ───────────────────────────────────────────────────
    private static final double PASSIVE_1_CRIT_RATE_PER_10_ATK = 0.01;    // 1%
    private static final double PASSIVE_1_CRIT_RATE_CAP         = 0.30;   // 30%

    // ── Passive 2 constants ───────────────────────────────────────────────────
    private static final double PASSIVE_2_CRIT_DMG_PER_10_ATK  = 0.02;    // 2%
    private static final double PASSIVE_2_CRIT_DMG_CAP          = 0.60;   // 60%

    // ── Passive 3 constants ───────────────────────────────────────────────────
    private static final double PASSIVE_3_HP_THRESHOLD = 0.25;   // 25% HP

    // ─────────────────────────────────────────────────────────────────────────

    public Zenzenkoi() {
        super("Zenzenkoi", 270, 15, 2, 700, 0.05, 0.50, 30);
    }

    // ── Passive slots ─────────────────────────────────────────────────────────

    @Override
    public Passive[] getPassives() {
        return new Passive[]{ passive1(), passive2(), passive3() };
    }

    private Passive passive1() {
        return new Passive() {
            // Stores the exact bonus applied so onBattleEnd removes the same value
            private double appliedBonus = 0.0;

            @Override public String getName()        { return "Sharpened Focus"; }
            @Override public String getDescription() {
                return "+1% Crit Rate per 10 Total ATK (max +30%)";
            }
            @Override public int getIntervalMs() { return 0; }

            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.BATTLE_START, PassiveEvent.BATTLE_END);
            }

            @Override
            public void onBattleStart(Entity owner, Combat.IBattle battle) {
                int totalAtk = owner.getEffectiveAtk();
                appliedBonus = Math.min(PASSIVE_1_CRIT_RATE_CAP,
                        Math.floor(totalAtk / 10.0) * PASSIVE_1_CRIT_RATE_PER_10_ATK);
                owner.addCritRate(appliedBonus);
            }

            @Override
            public void onBattleEnd(Entity owner, Combat.IBattle battle) {
                owner.addCritRate(-appliedBonus);
                appliedBonus = 0.0;
            }

            @Override public void trigger(PassiveContext ctx) { /* lifecycle-only */ }
        };
    }

    private Passive passive2() {
        return new Passive() {
            private double appliedBonus = 0.0;

            @Override public String getName()        { return "Lethal Edge"; }
            @Override public String getDescription() {
                return "+2% Crit Damage per 10 Total ATK (max +60%)";
            }
            @Override public int getIntervalMs() { return 0; }

            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.BATTLE_START, PassiveEvent.BATTLE_END);
            }

            @Override
            public void onBattleStart(Entity owner, Combat.IBattle battle) {
                int totalAtk = owner.getEffectiveAtk();
                appliedBonus = Math.min(PASSIVE_2_CRIT_DMG_CAP,
                        Math.floor(totalAtk / 10.0) * PASSIVE_2_CRIT_DMG_PER_10_ATK);
                owner.addCritDamage(appliedBonus);
            }

            @Override
            public void onBattleEnd(Entity owner, Combat.IBattle battle) {
                owner.addCritDamage(-appliedBonus);
                appliedBonus = 0.0;
            }

            @Override public void trigger(PassiveContext ctx) { /* lifecycle-only */ }
        };
    }

    private Passive passive3() {
        return new Passive() {
            private boolean triggered = false;

            @Override public String getName()        { return "Last Stand Strike"; }
            @Override public String getDescription() {
                return "At 25% HP: unleash one attack at 100 + 150% Total ATK (can crit, affected by shields and crit reduction). Once per battle.";
            }
            @Override public int getIntervalMs() { return 0; }

            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.ON_TAKE_DAMAGE, PassiveEvent.BATTLE_END);
            }

            @Override
            public void onBattleEnd(Entity owner, Combat.IBattle battle) {
                triggered = false;
            }

            @Override
            public void trigger(PassiveContext ctx) {
                if (ctx.event == PassiveEvent.BATTLE_END) return;
                if (triggered) return;
                if (!ctx.owner.isAlive()) return;

                // Check if HP will drop to/below 25% after this hit
                int hpAfterHit = ctx.owner.getCurrentHp() - ctx.incomingDamage;
                if ((double) hpAfterHit / ctx.owner.getMaxHp() > PASSIVE_3_HP_THRESHOLD) return;

                triggered = true;

                Entity target = ctx.battle.getOpponent(ctx.owner);
                if (target == null || !target.isAlive()) return;

                // ── Accuracy roll (same as calculateDamage) ───────────────────
                if (Math.random() >= ctx.owner.getAccuracy()) {
                    ctx.battle.notifyPassiveMiss(ctx.owner, target, getName());
                    return;
                }

                // ── Damage formula: flat 100 + 150% of total ATK ──────────────
                int totalAtk  = ctx.owner.getEffectiveAtk();
                double rawBase = 100 + (1.50 * totalAtk);

                // ── Crit roll ─────────────────────────────────────────────────
                boolean isCrit = Math.random() < ctx.owner.getCritRate();
                double critMultiplier = isCrit ? (1.0 + ctx.owner.getCritDamage()) : 1.0;
                double raw = rawBase * critMultiplier * (1.0 + ctx.owner.getDamageBonus());

                // ── DEF subtraction ───────────────────────────────────────────
                int dmgBeforePassives = Math.max(1, (int) raw - target.getDefense());

                // ── Route through ON_TAKE_DAMAGE so shields absorb it and crit
                //    reduction passives (e.g. Hanzo's Iron Resolve) apply ──────
                // We create a temporary PassiveContext for the defender side so
                // applyPassiveEvent works correctly with isCrit flag.
                int finalDmg = ctx.battle.applyPassiveEvent(
                        target, ctx.owner,
                        PassiveEvent.ON_TAKE_DAMAGE,
                        dmgBeforePassives, isCrit);

                // ── Apply and notify ──────────────────────────────────────────
                int actual = Math.min(finalDmg, target.getCurrentHp());
                target.takeDamage(finalDmg);

                String desc = (isCrit ? "★ CRIT! " : "")
                        + actual + " dmg at 25% HP (100 + 150% ATK)"
                        + (isCrit ? "!" : "");
                ctx.battle.notifyPassive(ctx.owner, target, getName(), desc, actual, false);
                ctx.battle.checkEndPublic();
            }
        };
    }

    // ── Level up ──────────────────────────────────────────────────────────────

    @Override
    public void levelUp() {
        switch (checkLevel()) {
            case 1 -> { maxHp += 3;  attack += 1; }
            case 2 -> { maxHp += 6;  attack += 2; }
            case 3 -> { maxHp += 9;  attack += 3; }
            case 4 -> { maxHp += 27; attack += 5; defense += 1; }
        }
    }

    // ── Sprite ────────────────────────────────────────────────────────────────

    @Override
    public SpriteSet getSpriteSet() {
        return new SpriteSet(
                SpritePaths.ZENZENKOI_IDLE,
                SpritePaths.ZENZENKOI_RUN,
                SpritePaths.ZENZENKOI_ATTACK,
                SpritePaths.ZENZENKOI_DEAD
        );
    }

    @Override
    public String toString() { return super.toString(); }
}