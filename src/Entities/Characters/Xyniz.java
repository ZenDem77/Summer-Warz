package Entities.Characters;

import Combat.DamageResult;
import Entities.Entity;
import Entities.Character;
import Entities.PassiveHandler.*;
import Entities.Sprites.*;

public class Xyniz extends Character {

    // ── Passive 1 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_1_BASE_DMG_MIN = 5;
    private static final int PASSIVE_1_BASE_DMG_MAX = 15;
    private static final int PASSIVE_1_INTERVAL_MS  = 1000;
    private        final int PASSIVE_1_EXTRA_DMG    = (int)(getEffectiveAtk() * 0.10);

    // ── Passive 2 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_2_HEAL_MIN     = 10;
    private static final int PASSIVE_2_HEAL_MAX     = 30;
    private static final int PASSIVE_2_INTERVAL_MS  = 2000;
    private final        int PASSIVE_2_EXTRA_HEAL   = (int)(getMaxHp() * 0.05);

    // ── Passive 3 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_3_BONUS_DAMAGE  = 50;
    private static final int PASSIVE_3_INTERVAL_MS   = 4000;
    private        final int PASSIVE_3_EXTRA_DMG     = (int)(getEffectiveAtk() * 1.40);

    public Xyniz() {
        super("Xyniz", 340, 16, 4, 500, 0.05, 0.50, 30);
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
            @Override public String getDescription() { return "Deal " + PASSIVE_1_EXTRA_DMG + " + " + PASSIVE_1_BASE_DMG_MIN + "-" + PASSIVE_1_BASE_DMG_MAX + " random True Damage every " + msToSec(PASSIVE_1_INTERVAL_MS); }
            @Override public int    getIntervalMs()  { return PASSIVE_1_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                int dmg = PASSIVE_1_BASE_DMG_MIN + (int)(Math.random() * (PASSIVE_1_BASE_DMG_MAX - PASSIVE_1_BASE_DMG_MIN + 1)) + PASSIVE_1_EXTRA_DMG;
                Entity target = ctx.battle.getOpponent(ctx.owner);
                DamageResult result = ctx.owner.calculateTrueDamage(dmg);
                if (result.isMiss) { ctx.battle.notifyPassiveMiss(ctx.owner, target, getName()); return; }
                int scaled = Math.max(1, (int)(result.amount * ctx.battle.getDamageMultiplier()));
                int actual = Math.min(scaled, target.getCurrentHp());
                target.takeDamage(scaled);
                ctx.battle.notifyPassive(ctx.owner, target, getName(),
                        actual + " random True Damage", actual, false);
                ctx.battle.checkEndPublic();
            }
        };
    }

    private Passive passive2() {
        return new Passive() {
            @Override public String getName()        { return "Iron Recovery"; }
            @Override public String getDescription() { return "Heal " + PASSIVE_2_EXTRA_HEAL + " + "+ PASSIVE_2_HEAL_MIN + "–" + PASSIVE_2_HEAL_MAX + " random HP every " + msToSec(PASSIVE_2_INTERVAL_MS); }
            @Override public int    getIntervalMs()  { return PASSIVE_2_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                int roll = PASSIVE_2_HEAL_MIN + (int)(Math.random() * (PASSIVE_2_HEAL_MAX - PASSIVE_2_HEAL_MIN + 1)) + PASSIVE_2_EXTRA_HEAL;
                int before = ctx.owner.getCurrentHp();
                ctx.owner.heal(roll);
                int healed = ctx.owner.getCurrentHp() - before;
                ctx.battle.notifyPassive(ctx.owner, ctx.owner, getName(),
                        "+" + healed + " HP restored", healed, true);
            }
        };
    }

    private Passive passive3() {
        return new Passive() {
            @Override public String getName()        { return "Iron Wrath"; }
            @Override public String getDescription() { return "Deal " + (PASSIVE_3_BONUS_DAMAGE + PASSIVE_3_EXTRA_DMG) + " True Damage every " + msToSec(PASSIVE_3_INTERVAL_MS); }
            @Override public int    getIntervalMs()  { return PASSIVE_3_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                Entity target = ctx.battle.getOpponent(ctx.owner);
                DamageResult result = ctx.owner.calculateTrueDamage(PASSIVE_3_BONUS_DAMAGE + PASSIVE_3_EXTRA_DMG);
                if (result.isMiss) { ctx.battle.notifyPassiveMiss(ctx.owner, target, getName()); return; }
                int dmg = Math.max(1, (int)(result.amount * ctx.battle.getDamageMultiplier()));
                int actual = Math.min(dmg, target.getCurrentHp());
                target.takeDamage(dmg);
                ctx.battle.notifyPassive(ctx.owner, target, getName(), actual + " True Damage", actual, false);
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
            case 4 -> { maxHp += 45; attack += 4; defense += 1; }
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