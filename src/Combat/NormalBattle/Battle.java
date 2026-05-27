package Combat.NormalBattle;

import Combat.DamageResult;
import Combat.IBattle;
import Entities.Character;
import Entities.Entity;
import Entities.Passive;
import Entities.PassiveContext;
import Entities.PassiveEvent;

import java.util.ArrayList;
import java.util.List;

public class Battle implements IBattle {

    public enum BattleState { IDLE, APPROACHING, ONGOING, PLAYER_WIN, ENEMY_WIN }

    public interface BattleListener {
        void onPlayerAttack(String logEntry, int damage, boolean isCrit, boolean isMiss);
        void onEnemyAttack(String logEntry, int damage, boolean isCrit, boolean isMiss);
        void onPassive(String logEntry, Entity owner, int amount, boolean isHeal);
        void onBattleEnd(BattleState result);
    }

    private final Entity player;
    private final Entity enemy;
    private BattleState state = BattleState.IDLE;

    private final List<BattleListener> listeners = new ArrayList<>();

    // All active passive instances, indexed for event dispatch
    private final List<Passive> playerPassives = new ArrayList<>();
    private final List<Passive> enemyPassives  = new ArrayList<>();

    // Periodic timers (only for passives with TICK)
    private final List<javax.swing.Timer> passiveTimers = new ArrayList<>();

    public Battle(Entity player, Entity enemy) {
        this.player = player;
        this.enemy  = enemy;
    }

    public void addListener(BattleListener l) { listeners.add(l); }

    public void start() {
        player.reset();
        enemy.reset();
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

    public void playerTick() {
        if (state != BattleState.ONGOING) return;
        DamageResult result = player.calculateDamage(enemy);
        if (result.isMiss) {
            String log = player.getName() + "'s attack missed!";
            for (BattleListener l : listeners) l.onPlayerAttack(log, 0, false, true);
            return;
        }
        // Let enemy's ON_TAKE_DAMAGE passives intercept
        int dmg = applyPassiveEvent(enemy, player, PassiveEvent.ON_TAKE_DAMAGE, result.amount, result.isCrit);
        // Let player's ON_DEAL_DAMAGE passives react (lifesteal etc.)
        applyPassiveEvent(player, enemy, PassiveEvent.ON_DEAL_DAMAGE, dmg, result.isCrit);

        enemy.takeDamage(dmg);
        String log = (result.isCrit ? "★ CRIT! " : "")
                + player.getName() + " hits " + enemy.getName()
                + " for " + dmg + " dmg! ("
                + enemy.getCurrentHp() + "/" + enemy.getMaxHp() + " HP)";
        for (BattleListener l : listeners) l.onPlayerAttack(log, dmg, result.isCrit, false);
        checkEnd();
    }

    public void enemyTick() {
        if (state != BattleState.ONGOING) return;
        DamageResult result = enemy.calculateDamage(player);
        if (result.isMiss) {
            String log = enemy.getName() + "'s attack missed!";
            for (BattleListener l : listeners) l.onEnemyAttack(log, 0, false, true);
            return;
        }
        // Let player's ON_TAKE_DAMAGE passives intercept
        int dmg = applyPassiveEvent(player, enemy, PassiveEvent.ON_TAKE_DAMAGE, result.amount, result.isCrit);
        // Let enemy's ON_DEAL_DAMAGE passives react
        applyPassiveEvent(enemy, player, PassiveEvent.ON_DEAL_DAMAGE, dmg, result.isCrit);

        player.takeDamage(dmg);
        String log = (result.isCrit ? "★ CRIT! " : "")
                + enemy.getName() + " hits " + player.getName()
                + " for " + dmg + " dmg! ("
                + player.getCurrentHp() + "/" + player.getMaxHp() + " HP)";
        for (BattleListener l : listeners) l.onEnemyAttack(log, dmg, result.isCrit, false);
        checkEnd();
    }

    // ── IBattle: passive event dispatch ───────────────────────────────────────

    /**
     * Fans out the event to all passives on `owner` that subscribe to it.
     * Returns the (possibly modified) damage after all passives have run.
     */
    @Override
    public int applyPassiveEvent(Entity owner, Entity target,
                                 PassiveEvent event, int damage, boolean isCrit) {
        List<Passive> passives = (owner == player) ? playerPassives : enemyPassives;
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
        playerPassives.clear();
        enemyPassives.clear();

        if (player instanceof Character c) {
            playerPassives.addAll(c.getActivePassives());
        } else {
            Passive pp = player.getPassive();
            if (pp != null) playerPassives.add(pp);
        }

        Passive ep = enemy.getPassive();
        if (ep != null) enemyPassives.add(ep);
    }

    private void fireLifecycle(PassiveEvent event) {
        for (Passive p : playerPassives) {
            if (event == PassiveEvent.BATTLE_START) p.onBattleStart(player, this);
            else if (event == PassiveEvent.BATTLE_END) p.onBattleEnd(player, this);
        }
        for (Passive p : enemyPassives) {
            if (event == PassiveEvent.BATTLE_START) p.onBattleStart(enemy, this);
            else if (event == PassiveEvent.BATTLE_END) p.onBattleEnd(enemy, this);
        }
    }

    private void startTickTimers() {
        for (Passive p : playerPassives) {
            if (p.respondsTo().contains(PassiveEvent.TICK) && p.getIntervalMs() > 0) {
                javax.swing.Timer t = new javax.swing.Timer(p.getIntervalMs(), e -> {
                    PassiveContext ctx = new PassiveContext(
                            player, enemy, this, PassiveEvent.TICK, 0, false);
                    p.trigger(ctx);
                });
                t.start();
                passiveTimers.add(t);
            }
        }
        for (Passive p : enemyPassives) {
            if (p.respondsTo().contains(PassiveEvent.TICK) && p.getIntervalMs() > 0) {
                javax.swing.Timer t = new javax.swing.Timer(p.getIntervalMs(), e -> {
                    PassiveContext ctx = new PassiveContext(
                            enemy, player, this, PassiveEvent.TICK, 0, false);
                    p.trigger(ctx);
                });
                t.start();
                passiveTimers.add(t);
            }
        }
    }

    // ── IBattle ───────────────────────────────────────────────────────────────

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
        if (!enemy.isAlive()) {
            state = BattleState.PLAYER_WIN;
            fireLifecycle(PassiveEvent.BATTLE_END);
            stop();
            for (BattleListener l : listeners) l.onBattleEnd(state);
        } else if (!player.isAlive()) {
            state = BattleState.ENEMY_WIN;
            fireLifecycle(PassiveEvent.BATTLE_END);
            stop();
            for (BattleListener l : listeners) l.onBattleEnd(state);
        }
    }

    public BattleState getState() { return state; }
    public Entity getPlayer()     { return player; }
    public Entity getEnemy()      { return enemy; }
}