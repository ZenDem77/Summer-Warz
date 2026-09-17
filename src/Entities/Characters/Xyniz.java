package Entities.Characters;

import Combat.DamageResult;
import Entities.Entity;
import Entities.Character;
import Entities.PassiveHandler.*;
import Entities.Sprites.*;

public class Xyniz extends Character {

    // ── Passive 1 constants ───────────────────────────────────────────────────
    private static final double PASSIVE_1_ATK_PERCENT  = 0.15;
    private static final int    PASSIVE_1_INTERVAL_MS  = 800;

    // ── Passive 2 constants ───────────────────────────────────────────────────
    private static final double PASSIVE_2_HEAL_PERCENT = 0.1;
    private static final int    PASSIVE_2_INTERVAL_MS  = 2000;

    // ── Passive 3 constants ───────────────────────────────────────────────────
    private static final double PASSIVE_3_ATK_PERCENT  = 1.40;
    private static final int    PASSIVE_3_INTERVAL_MS  = 2500;

    public Xyniz() {
        super("Xyniz", 340, 16, 4, 500, 0.05, 1.0, 30);
    }

    // ── Passive slots ─────────────────────────────────────────────────────────

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
            @Override public String getName()        { return "Chaos Strike"; }
            @Override public String getDescription() {
                return "Deal 15% Total ATK true damage every " + msToSec(PASSIVE_1_INTERVAL_MS);
            }
            @Override public int getIntervalMs() { return PASSIVE_1_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                Entity target = ctx.battle.getOpponent(ctx.owner);
                int dmg = Math.max(1, (int)(PASSIVE_1_ATK_PERCENT * ctx.owner.getEffectiveAtk()));
                DamageResult result = ctx.owner.calculateTrueDamage(dmg);
                if (result.isMiss) {
                    ctx.battle.notifyPassiveMiss(ctx.owner, target, getName());
                    return;
                }
                int scaled = Math.max(1, (int)(result.amount * ctx.battle.getDamageMultiplier()));
                int actual = ctx.battle.applyTrueDamageRaw(ctx.owner, target, scaled, getName());
                if (actual < 0) return; // immune — notifyImmune already fired inside
                ctx.battle.notifyPassive(ctx.owner, target, getName(),
                        actual + " true damage ", actual, false);
                ctx.battle.checkEndPublic();
            }
        };
    }

    private Passive passive2() {
        return new Passive() {
            @Override public String getName()        { return "Iron Recovery"; }
            @Override public String getDescription() {
                return "Heal 10% max HP every " + msToSec(PASSIVE_2_INTERVAL_MS);
            }
            @Override public int getIntervalMs() { return PASSIVE_2_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                int amount = Math.max(1, (int)(ctx.owner.getMaxHp() * PASSIVE_2_HEAL_PERCENT));
                int before = ctx.owner.getCurrentHp();
                ctx.owner.heal(amount);
                int healed = ctx.owner.getCurrentHp() - before;
                ctx.battle.notifyPassive(ctx.owner, ctx.owner, getName(),
                        "+" + healed + " HP restored ", healed, true);
            }
        };
    }

    private Passive passive3() {
        return new Passive() {
            @Override public String getName()        { return "Iron Wrath"; }
            @Override public String getDescription() { return "Deal 140% Total ATK true dmg every " + msToSec(PASSIVE_3_INTERVAL_MS); }
            @Override public int getIntervalMs() { return PASSIVE_3_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                Entity target = ctx.battle.getOpponent(ctx.owner);
                int dmg = Math.max(1, (int)(PASSIVE_3_ATK_PERCENT * ctx.owner.getEffectiveAtk()));
                DamageResult result = ctx.owner.calculateTrueDamage(dmg);
                if (result.isMiss) {
                    ctx.battle.notifyPassiveMiss(ctx.owner, target, getName());
                    return;
                }
                int scaled = Math.max(1, (int)(result.amount * ctx.battle.getDamageMultiplier()));
                int actual = ctx.battle.applyTrueDamageRaw(ctx.owner, target, scaled, getName());
                if (actual < 0) return; // immune — notifyImmune already fired inside
                ctx.battle.notifyPassive(ctx.owner, target, getName(),
                        actual + " true damage ", actual, false);
                ctx.battle.checkEndPublic();
            }
        };
    }

    // ── Level up ──────────────────────────────────────────────────────────────
    @Override
    public void levelUp() {
        switch (checkLevel()) {
            case 1 -> { maxHp += 8;  attack += 1; }
            case 2 -> { maxHp += 12; attack += 2; }
            case 3 -> { maxHp += 16; attack += 3; }
            case 4 -> { maxHp += 48; attack += 4; defense += 1; }
        }
    }

    // ── Sprite ────────────────────────────────────────────────────────────────
    @Override
    public SpriteSet getSpriteSet() {
        return new SpriteSet(
                SpritePaths.XYNIZ_IDLE,
                SpritePaths.XYNIZ_RUN,
                SpritePaths.XYNIZ_ATTACK,
                SpritePaths.XYNIZ_DEAD,
                SpritePaths.XYNIZ_PROJECTILE
        );
    }

    @Override
    public String toString() { return super.toString(); }
}