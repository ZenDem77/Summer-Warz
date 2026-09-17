package Entities.Characters;

import Entities.Character;
import Entities.PassiveHandler.*;
import Entities.Sprites.SpritePaths;
import Entities.Sprites.SpriteSet;

import java.util.EnumSet;
import java.util.Set;

public class Kouzen extends Character implements Shielded {

    // ── Passive 1 constants ───────────────────────────────────────────────────
    private static final double PASSIVE_1_SHIELD_PERCENT = 0.80;   // 80% of max HP

    // ── Passive 2 constants ───────────────────────────────────────────────────
    private static final double PASSIVE_2_DMG_REDUCTION  = 0.12;   // 12% reduction

    // ── Passive 3 constants ───────────────────────────────────────────────────
    private static final double PASSIVE_3_SHIELD_PERCENT = 0.10;   // 10% of max HP
    private static final int    PASSIVE_3_INTERVAL_MS    = 2000;

    // ── Shield state ─────────────────────────────────────────────────────────
    private int shieldHp = 0;

    // ── Constructor ───────────────────────────────────────────────────────────

    public Kouzen() {
        super("Kouzen", 370, 15, 10, 800, 0.05, 0.50, 30);
    }

    // ── Shielded interface ────────────────────────────────────────────────────

    @Override
    public int getShieldHp() { return shieldHp; }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @Override
    public void reset() {
        super.reset();
        shieldHp = 0;
    }

    // ── Passive slots ─────────────────────────────────────────────────────────

    @Override
    public Passive[] getPassives() {
        return new Passive[]{ passive1(), passive2(), passive3() };
    }

    private Passive passive1() {
        return new Passive() {
            private boolean triggered = false;

            @Override public String getName() { return "Iron Veil"; }
            @Override public String getDescription() {
                return "At battle start: gain shield equal to 80% max HP. "
                        + "Shield absorbs damage before HP. Once per battle.";
            }
            @Override public int getIntervalMs() { return 0; }

            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.BATTLE_START,
                        PassiveEvent.BATTLE_END,
                        PassiveEvent.ON_TAKE_DAMAGE);
            }

            @Override
            public void onBattleStart(Entities.Entity owner, Combat.IBattle battle) {
                if (triggered) return;
                triggered = true;
                int grant = (int)(owner.getMaxHp() * PASSIVE_1_SHIELD_PERCENT);
                shieldHp += grant;
                battle.notifyShield(owner, getName(), grant);
            }

            @Override
            public void onBattleEnd(Entities.Entity owner, Combat.IBattle battle) {
                triggered = false;
            }

            @Override
            public void trigger(PassiveContext ctx) {
                if (ctx.event != PassiveEvent.ON_TAKE_DAMAGE) return;
                if (shieldHp <= 0 || ctx.incomingDamage <= 0) return;

                if (ctx.incomingDamage <= shieldHp) {
                    shieldHp -= ctx.incomingDamage;
                    ctx.incomingDamage = 0;
                } else {
                    ctx.incomingDamage -= shieldHp;
                    shieldHp = 0;
                }
            }
        };
    }

    private Passive passive2() {
        return new Passive() {
            @Override public String getName() { return "Composite Guard"; }
            @Override public String getDescription() {
                return (int)(PASSIVE_2_DMG_REDUCTION * 100)
                        + "% reduction on all incoming damage (applied after shield absorption).";
            }
            @Override public int getIntervalMs() { return 0; }

            @Override
            public Set<PassiveEvent> respondsTo() {
                return EnumSet.of(PassiveEvent.ON_TAKE_DAMAGE);
            }

            @Override
            public void trigger(PassiveContext ctx) {
                if (ctx.incomingDamage <= 0) return;
                int reduced = Math.max(1,
                        (int)(ctx.incomingDamage * (1.0 - PASSIVE_2_DMG_REDUCTION)));
                ctx.incomingDamage = reduced;
            }
        };
    }

    private Passive passive3() {
        return new Passive() {
            @Override public String getName() { return "Reinforced Will"; }
            @Override public String getDescription() {
                return "Gain shield equal to 10% max HP every 2s (stacks).";
            }
            @Override public int getIntervalMs() { return PASSIVE_3_INTERVAL_MS; }

            @Override
            public void trigger(PassiveContext ctx) {
                if (!ctx.owner.isAlive()) return;
                int grant = Math.max(1, (int)(ctx.owner.getMaxHp() * PASSIVE_3_SHIELD_PERCENT));
                shieldHp += grant;
                ctx.battle.notifyShield(ctx.owner, getName(), grant);
            }
        };
    }

    // ── Level up ──────────────────────────────────────────────────────────────

    @Override
    public void levelUp() {
        switch (checkLevel()) {
            case 1 -> { maxHp += 8;  attack += 1; }
            case 2 -> { maxHp += 16;  attack += 1; }
            case 3 -> { maxHp += 24; attack += 1; }
            case 4 -> { maxHp += 72; attack += 2; defense += 1; }
        }
    }

    // ── Sprite ────────────────────────────────────────────────────────────────

    @Override
    public SpriteSet getSpriteSet() {
        return new SpriteSet(
                SpritePaths.KOUZEN_IDLE,
                SpritePaths.KOUZEN_RUN,
                SpritePaths.KOUZEN_ATTACK,
                SpritePaths.KOUZEN_DEAD
        );
    }

    @Override
    public String toString() { return super.toString(); }
}