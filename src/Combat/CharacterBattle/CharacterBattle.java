package Combat.CharacterBattle;

import Combat.DamageResult;
import Combat.IBattle;
import Entities.Character;
import Entities.Entity;
import Entities.PassiveHandler.*;

import java.util.ArrayList;
import java.util.List;

public class CharacterBattle implements IBattle {

    public enum BattleState { IDLE, APPROACHING, ONGOING, FIGHTER1_WIN, FIGHTER2_WIN }

    // ── Damage reduction ──────────────────────────────────────────────────────
    // CharacterBattle is used exclusively for character-vs-character modes
    // (Spar). Since both sides are full player characters with passives,
    // weapons, and artifacts, raw damage numbers would be far too lethal —
    // so ALL damage (direct attacks AND true-damage passives) is reduced
    // by this percentage. To customize: just change this one number.
    public static final double DAMAGE_REDUCTION  = 0.80;   // 80% reduction
    public static final double DAMAGE_MULTIPLIER = 1.0 - DAMAGE_REDUCTION;

    @Override
    public double getDamageMultiplier() { return DAMAGE_MULTIPLIER; }

    public interface BattleListener {
        void onFighter1Attack(String logEntry, int damage, boolean isCrit, boolean isMiss);
        void onFighter2Attack(String logEntry, int damage, boolean isCrit, boolean isMiss);
        void onPassive(String logEntry, Entity owner, int amount, boolean isHeal);
        /** Fired when a passive's true-damage attack misses (accuracy roll failed). */
        void onPassiveMiss(String logEntry, Entity owner, Entity target, String passiveName);
        /** Fired when a passive deals a special hit (e.g. Zenzenkoi's Last Stand Strike). */
        void onSpecialHit(String logEntry, Entity owner, Entity target, int amount, boolean isCrit);
        /** Fired when a passive's true damage is blocked by the target's immunity. */
        void onImmune(String logEntry, Entity owner, Entity target, String passiveName);
        void onBattleEnd(BattleState result);
    }

    private final Character fighter1;
    private final Character fighter2;
    private BattleState state = BattleState.IDLE;

    private final List<BattleListener> listeners = new ArrayList<>();

    private final List<Passive> f1Passives = new ArrayList<>();
    private final List<Passive> f2Passives = new ArrayList<>();
    private final List<javax.swing.Timer> passiveTimers = new ArrayList<>();

    public CharacterBattle(Character fighter1, Character fighter2) {
        this.fighter1 = fighter1;
        this.fighter2 = fighter2;
    }

    public void addListener(BattleListener l) { listeners.add(l); }

    public void start() {
        fighter1.reset();
        fighter2.reset();
        state = BattleState.APPROACHING;
    }

    public void setEngaged() {
        if (state != BattleState.APPROACHING) return;
        state = BattleState.ONGOING;
        collectPassives();
        fireLifecycle(PassiveEvent.BATTLE_START);
        startTickTimers();
    }

    public void stop() {
        passiveTimers.forEach(javax.swing.Timer::stop);
        passiveTimers.clear();
    }

    // ── Attack ticks ──────────────────────────────────────────────────────────

    public void fighter1Tick() {
        if (state != BattleState.ONGOING) return;
        DamageResult result = fighter1.calculateDamage(fighter2);
        if (result.isMiss) {
            String log = fighter1.getName() + "'s attack missed!";
            for (BattleListener l : listeners) l.onFighter1Attack(log, 0, false, true);
            return;
        }
        int scaled = Math.max(1, (int)(result.amount * getDamageMultiplier()));
        int dmg = applyPassiveEvent(fighter2, fighter1, PassiveEvent.ON_TAKE_DAMAGE, scaled, result.isCrit);
        applyPassiveEvent(fighter1, fighter2, PassiveEvent.ON_DEAL_DAMAGE, dmg, result.isCrit);

        fighter2.takeDamage(dmg);
        String log = (result.isCrit ? "★ CRIT! " : "")
                + fighter1.getName() + " hits " + fighter2.getName()
                + " for " + dmg + " dmg! ("
                + fighter2.getCurrentHp() + "/" + fighter2.getMaxHp() + " HP)";
        for (BattleListener l : listeners) l.onFighter1Attack(log, dmg, result.isCrit, false);
        checkEnd();
    }

    public void fighter2Tick() {
        if (state != BattleState.ONGOING) return;
        DamageResult result = fighter2.calculateDamage(fighter1);
        if (result.isMiss) {
            String log = fighter2.getName() + "'s attack missed!";
            for (BattleListener l : listeners) l.onFighter2Attack(log, 0, false, true);
            return;
        }
        int scaled = Math.max(1, (int)(result.amount * getDamageMultiplier()));
        int dmg = applyPassiveEvent(fighter1, fighter2, PassiveEvent.ON_TAKE_DAMAGE, scaled, result.isCrit);
        applyPassiveEvent(fighter2, fighter1, PassiveEvent.ON_DEAL_DAMAGE, dmg, result.isCrit);

        fighter1.takeDamage(dmg);
        String log = (result.isCrit ? "★ CRIT! " : "")
                + fighter2.getName() + " hits " + fighter1.getName()
                + " for " + dmg + " dmg! ("
                + fighter1.getCurrentHp() + "/" + fighter1.getMaxHp() + " HP)";
        for (BattleListener l : listeners) l.onFighter2Attack(log, dmg, result.isCrit, false);
        checkEnd();
    }

    // ── IBattle: passive event dispatch ───────────────────────────────────────

    @Override
    public int applyPassiveEvent(Entity owner, Entity target,
                                 PassiveEvent event, int damage, boolean isCrit) {
        List<Passive> passives = (owner == fighter1) ? f1Passives : f2Passives;
        PassiveContext ctx = new PassiveContext(owner, target, this, event, damage, isCrit);
        for (Passive p : passives) {
            if (p.respondsTo().contains(event)) {
                p.trigger(ctx);
            }
        }
        return ctx.incomingDamage;
    }

    // ── Passive lifecycle ─────────────────────────────────────────────────────

    private void collectPassives() {
        f1Passives.clear();
        f2Passives.clear();
        f1Passives.addAll(fighter1.getActivePassives());
        f2Passives.addAll(fighter2.getActivePassives());
    }

    private void fireLifecycle(PassiveEvent event) {
        for (Passive p : f1Passives) {
            if (event == PassiveEvent.BATTLE_START) p.onBattleStart(fighter1, this);
            else if (event == PassiveEvent.BATTLE_END) p.onBattleEnd(fighter1, this);
        }
        for (Passive p : f2Passives) {
            if (event == PassiveEvent.BATTLE_START) p.onBattleStart(fighter2, this);
            else if (event == PassiveEvent.BATTLE_END) p.onBattleEnd(fighter2, this);
        }
    }

    private void startTickTimers() {
        for (Passive p : f1Passives) {
            if (p.respondsTo().contains(PassiveEvent.TICK) && p.getIntervalMs() > 0) {
                javax.swing.Timer t = new javax.swing.Timer(p.getIntervalMs(), e -> {
                    PassiveContext ctx = new PassiveContext(
                            fighter1, fighter2, this, PassiveEvent.TICK, 0, false);
                    p.trigger(ctx);
                });
                t.start();
                passiveTimers.add(t);
            }
        }
        for (Passive p : f2Passives) {
            if (p.respondsTo().contains(PassiveEvent.TICK) && p.getIntervalMs() > 0) {
                javax.swing.Timer t = new javax.swing.Timer(p.getIntervalMs(), e -> {
                    PassiveContext ctx = new PassiveContext(
                            fighter2, fighter1, this, PassiveEvent.TICK, 0, false);
                    p.trigger(ctx);
                });
                t.start();
                passiveTimers.add(t);
            }
        }
    }

    // ── IBattle ───────────────────────────────────────────────────────────────

    @Override
    public void notifyShield(Entity owner, String passiveName, int amount) {
        String total = (owner instanceof Shielded s)
                ? " (total: " + s.getShieldHp() + ")" : "";
        String log = "[" + passiveName + "] " + owner.getName()
                + " — +" + amount + " shield" + total;
        for (BattleListener l : listeners) l.onPassive(log, owner, amount, true);
    }

    @Override
    public void notifyPassiveMiss(Entity owner, Entity target, String passiveName) {
        String log = "[" + passiveName + "] " + owner.getName() + "'s attack missed " + target.getName() + "!";
        for (BattleListener l : listeners) l.onPassiveMiss(log, owner, target, passiveName);
    }

    @Override
    public void notifySpecialHit(Entity owner, Entity target, String passiveName,
                                 String effectDesc, int amount, boolean isCrit) {
        String log = "[" + passiveName + "] " + (isCrit ? "★ CRIT! " : "")
                + owner.getName() + " — " + effectDesc
                + " (" + target.getName() + ": "
                + target.getCurrentHp() + "/" + target.getMaxHp() + " HP)";
        for (BattleListener l : listeners) l.onSpecialHit(log, owner, target, amount, isCrit);
    }

    @Override
    public void notifyImmune(Entity owner, Entity target, String passiveName) {
        String log = "[" + passiveName + "] " + target.getName() + " is immune to true damage!";
        for (BattleListener l : listeners) l.onImmune(log, owner, target, passiveName);
    }

    @Override
    public void notifyPassive(Entity owner, Entity target, String passiveName,
                              String effectDesc, int amount, boolean isHeal) {
        String log = "[" + passiveName + "] " + owner.getName() + " — " + effectDesc
                + " (" + target.getName() + ": "
                + target.getCurrentHp() + "/" + target.getMaxHp() + " HP)";
        for (BattleListener l : listeners) l.onPassive(log, owner, amount, isHeal);
    }

    @Override
    public void checkEndPublic() { checkEnd(); }

    private void checkEnd() {
        if (!fighter2.isAlive()) {
            state = BattleState.FIGHTER1_WIN;
            fireLifecycle(PassiveEvent.BATTLE_END);
            stop();
            for (BattleListener l : listeners) l.onBattleEnd(state);
        } else if (!fighter1.isAlive()) {
            state = BattleState.FIGHTER2_WIN;
            fireLifecycle(PassiveEvent.BATTLE_END);
            stop();
            for (BattleListener l : listeners) l.onBattleEnd(state);
        }
    }

    public BattleState getState()    { return state; }
    public Character   getFighter1() { return fighter1; }
    public Character   getFighter2() { return fighter2; }
    public Character   getPlayer()   { return fighter1; }
    public Character   getEnemy()    { return fighter2; }
}