package Combat.NormalBattle;

import Combat.DamageResult;
import Entities.Entity;
import Entities.Character;
import Entities.Passive;

import java.util.ArrayList;
import java.util.List;

public class Battle {

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

    // Passive timers (managed here so Battle owns the lifecycle)
    private final List<javax.swing.Timer> playerPassiveTimers = new ArrayList<>();
    private javax.swing.Timer enemyPassiveTimer;

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
        startPassiveTimers();
    }

    public void stop() {
        playerPassiveTimers.forEach(javax.swing.Timer::stop);
        if (enemyPassiveTimer != null) enemyPassiveTimer.stop();
    }

    // ── Attack ticks ──────────────────────────────────────────────────────────

    public void playerTick() {
        if (state != BattleState.ONGOING) return;
        DamageResult result = player.calculateDamage(enemy);
        String log;
        if (result.isMiss) {
            log = player.getName() + "'s attack missed!";
        } else {
            enemy.takeDamage(result.amount);
            log = (result.isCrit ? "★ CRIT! " : "")
                    + player.getName() + " hits " + enemy.getName()
                    + " for " + result.amount + " dmg! ("
                    + enemy.getCurrentHp() + "/" + enemy.getMaxHp() + " HP)";
        }
        for (BattleListener l : listeners) l.onPlayerAttack(log, result.amount, result.isCrit, result.isMiss);
        checkEnd();
    }

    public void enemyTick() {
        if (state != BattleState.ONGOING) return;
        DamageResult result = enemy.calculateDamage(player);
        String log;
        if (result.isMiss) {
            log = enemy.getName() + "'s attack missed!";
        } else {
            player.takeDamage(result.amount);
            log = (result.isCrit ? "★ CRIT! " : "")
                    + enemy.getName() + " hits " + player.getName()
                    + " for " + result.amount + " dmg! ("
                    + player.getCurrentHp() + "/" + player.getMaxHp() + " HP)";
        }
        for (BattleListener l : listeners) l.onEnemyAttack(log, result.amount, result.isCrit, result.isMiss);
        checkEnd();
    }

    // ── Passive support ───────────────────────────────────────────────────────

    private void startPassiveTimers() {
        // Characters have up to 3 level-gated passive slots
        if (player instanceof Character c) {
            for (Passive pp : c.getActivePassives()) {
                javax.swing.Timer t = new javax.swing.Timer(pp.getIntervalMs(),
                        e -> pp.trigger(player, this));
                t.start();
                playerPassiveTimers.add(t);
            }
        } else {
            // Fallback: plain Entity passive (e.g. enemy used as player)
            Passive pp = player.getPassive();
            if (pp != null) {
                javax.swing.Timer t = new javax.swing.Timer(pp.getIntervalMs(),
                        e -> pp.trigger(player, this));
                t.start();
                playerPassiveTimers.add(t);
            }
        }

        // Enemies use a single passive
        Passive ep = enemy.getPassive();
        if (ep != null) {
            enemyPassiveTimer = new javax.swing.Timer(ep.getIntervalMs(),
                    e -> ep.trigger(enemy, this));
            enemyPassiveTimer.start();
        }
    }

    /** Called by Passive implementations to broadcast a passive event. */
    public void notifyPassive(Entity owner, Entity target, String passiveName,
                              String effectDesc, int amount, boolean isHeal) {
        String log = "[" + passiveName + "] " + owner.getName() + " — " + effectDesc
                + " (" + target.getName() + ": "
                + target.getCurrentHp() + "/" + target.getMaxHp() + " HP)";
        for (BattleListener l : listeners) l.onPassive(log, owner, amount, isHeal);
    }

    /** Exposed so Passive implementations can trigger win-check after dealing damage. */
    public void checkEndPublic() { checkEnd(); }

    private void checkEnd() {
        if (!enemy.isAlive()) {
            state = BattleState.PLAYER_WIN;
            stop();
            for (BattleListener l : listeners) l.onBattleEnd(state);
        } else if (!player.isAlive()) {
            state = BattleState.ENEMY_WIN;
            stop();
            for (BattleListener l : listeners) l.onBattleEnd(state);
        }
    }

    public BattleState getState() { return state; }
    public Entity getPlayer()     { return player; }
    public Entity getEnemy()      { return enemy; }
}