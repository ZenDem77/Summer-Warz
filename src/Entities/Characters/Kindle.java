package Entities.Characters;

import Combat.DamageResult;
import Entities.Entity;
import Entities.Character;
import Entities.PassiveHandler.*;
import Entities.Sprites.SpritePaths;
import Entities.Sprites.SpriteSet;

public class Kindle extends Character {

    // ── Passive 1 constants ───────────────────────────────────────────────────
    private static final double PASSIVE_1_ATK_PERCENT  = 0.90;
    private static final int    PASSIVE_1_INTERVAL_MS  = 3000;

    // ── Passive 2 constants ───────────────────────────────────────────────────
    private static final double PASSIVE_2_ATK_PERCENT  = 0.20;
    private static final int    PASSIVE_2_INTERVAL_MS  = 500;

    // ── Passive 3 constants ───────────────────────────────────────────────────
    private static final int PASSIVE_3_BONUS_DAMAGE = 15;
    private static final int PASSIVE_3_HEAL_AMOUNT  = 10;
    private static final int PASSIVE_3_INTERVAL_MS  = 450;

    public Kindle() {
        super("Kindle", 220, 27, 5, 400, 0.05, 1.0, 30);
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
            @Override public String getName()        { return "Ember Surge"; }
            @Override public String getDescription() { return "Deal 90% Total ATK true damage every " + msToSec(PASSIVE_1_INTERVAL_MS); }
            @Override public int getIntervalMs() { return PASSIVE_1_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                Entity target = ctx.battle.getOpponent(ctx.owner);
                int dmg = (int)(PASSIVE_1_ATK_PERCENT * ctx.owner.getEffectiveAtk());
                DamageResult result = ctx.owner.calculateTrueDamage(dmg);
                if (result.isMiss) { ctx.battle.notifyPassiveMiss(ctx.owner, target, getName()); return; }
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
            @Override public String getName()        { return "Cinder Echo"; }
            @Override public String getDescription() { return "Deal 20% Total ATK true damage every " + msToSec(PASSIVE_2_INTERVAL_MS); }
            @Override public int getIntervalMs() { return PASSIVE_2_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                Entity target = ctx.battle.getOpponent(ctx.owner);
                int dmg = (int)(PASSIVE_2_ATK_PERCENT * ctx.owner.getEffectiveAtk());
                DamageResult result = ctx.owner.calculateTrueDamage(Math.max(1, dmg));
                if (result.isMiss) { ctx.battle.notifyPassiveMiss(ctx.owner, target, getName()); return; }
                int scaled = Math.max(1, (int)(result.amount * ctx.battle.getDamageMultiplier()));
                int actual = ctx.battle.applyTrueDamageRaw(ctx.owner, target, scaled, getName());
                if (actual < 0) return; // immune — notifyImmune already fired inside
                ctx.battle.notifyPassive(ctx.owner, target, getName(),
                        actual + " true damage ", actual, false);
                ctx.battle.checkEndPublic();
            }
        };
    }

    private Passive passive3() {
        return new Passive() {
            @Override public String getName()        { return "Ember Mend"; }
            @Override public String getDescription() {
                return "Deal " + PASSIVE_3_BONUS_DAMAGE + " true damage and heal "
                        + PASSIVE_3_HEAL_AMOUNT + " HP every " + msToSec(PASSIVE_3_INTERVAL_MS);
            }
            @Override public int getIntervalMs() { return PASSIVE_3_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                Entity target = ctx.battle.getOpponent(ctx.owner);
                DamageResult result = ctx.owner.calculateTrueDamage(PASSIVE_3_BONUS_DAMAGE);
                if (!result.isMiss) {
                    int scaled = Math.max(1, (int)(result.amount * ctx.battle.getDamageMultiplier()));
                    int actual = ctx.battle.applyTrueDamageRaw(ctx.owner, target, scaled, getName());
                    if (actual >= 0) {
                        ctx.battle.notifyPassive(ctx.owner, target, getName(),
                                actual + " true damage ", actual, false);
                        ctx.battle.checkEndPublic();
                    }
                }
                if (ctx.owner.isAlive()) {
                    int before = ctx.owner.getCurrentHp();
                    ctx.owner.heal(PASSIVE_3_HEAL_AMOUNT);
                    int healed = ctx.owner.getCurrentHp() - before;
                    ctx.battle.notifyPassive(ctx.owner, ctx.owner, getName(),
                            "+" + healed + " HP restored", healed, true);
                }
            }
        };
    }

    // ── Level up ──────────────────────────────────────────────────────────────
    @Override
    public void levelUp() {
        switch (checkLevel()) {
            case 1 -> { maxHp += 4;  attack += 1; }
            case 2 -> { maxHp += 8;  attack += 2; }
            case 3 -> { maxHp += 12; attack += 3; }
            case 4 -> { maxHp += 36; attack += 5; defense += 1; }
        }
    }

    // ── Sprite ────────────────────────────────────────────────────────────────
    @Override
    public SpriteSet getSpriteSet() {
        return new SpriteSet(
                SpritePaths.KINDLE_IDLE,
                SpritePaths.KINDLE_RUN,
                SpritePaths.KINDLE_ATTACK,
                SpritePaths.KINDLE_DEAD
        );
    }

    @Override
    public String toString() { return super.toString(); }
}