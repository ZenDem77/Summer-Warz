package Combat.NormalBattle;

import Combat.DamageResult;
import Combat.IBattle;
import Entities.*;
import Entities.Character;
import Entities.PassiveHandler.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Battle — N Characters (player dojo) vs M Enemies.
 *
 * Combat is always 1v1 between the current active fighters.
 * When an active fighter dies the next one in their team's queue
 * approaches and joins the fight. Battle ends when all fighters
 * on one side are defeated.
 */
public class Battle implements IBattle {

    public enum BattleState { IDLE, APPROACHING, ONGOING, PLAYER_WIN, ENEMY_WIN }

    public interface BattleListener {
        void onPlayerAttack(String logEntry, int damage, boolean isCrit, boolean isMiss);
        void onEnemyAttack(String logEntry, int damage, boolean isCrit, boolean isMiss);
        void onPassive(String logEntry, Entity owner, int amount, boolean isHeal);
        /** Fired when a passive's true-damage attack misses (accuracy roll failed). */
        void onPassiveMiss(String logEntry, Entity owner, Entity target, String passiveName);
        /** Fired when a passive deals a special hit (distinct visual from generic passive damage). */
        void onSpecialHit(String logEntry, Entity owner, Entity target, int amount, boolean isCrit);
        /** Fired when the active fighter on a side changes (next fighter enters). */
        void onFighterEnter(boolean isPlayer, Entity fighter, int remaining);
        void onBattleEnd(BattleState result);
    }

    // ── Teams ─────────────────────────────────────────────────────────────────
    private final List<Character> playerTeam;
    private final List<Enemy>     enemyTeam;

    private int playerIndex = 0;   // index of the active player fighter
    private int enemyIndex  = 0;   // index of the active enemy fighter

    // ── Damage tracking ───────────────────────────────────────────────────────
    // Keyed by Character instance; accumulated throughout the whole battle.
    private final java.util.LinkedHashMap<Character, Integer> damageDealt = new java.util.LinkedHashMap<>();
    // Starts as sum of all enemies' maxHp; grows whenever an enemy heals.
    private int enemyHpPool = 0;

    private BattleState state = BattleState.IDLE;
    private final List<BattleListener> listeners = new ArrayList<>();

    // ── Passives for the current active fighters ──────────────────────────────
    private final List<Passive> playerPassives = new ArrayList<>();
    private final List<Passive> enemyPassives  = new ArrayList<>();
    private final List<javax.swing.Timer> passiveTimers = new ArrayList<>();

    // ── Pausable timer registry ───────────────────────────────────────────────
    // Passive lifecycle timers (e.g. Zayir's 2s ATK boost) register here so
    // pause()/resume() can include them alongside the recurring tick timers.
    private final List<javax.swing.Timer> pausableTimers    = new ArrayList<>();
    // Tracks exactly which timers WE stopped on pause() so resume() only
    // restarts those — avoids accidentally restarting a one-shot timer that
    // had already fired and stopped naturally before the pause.
    private final java.util.Set<javax.swing.Timer> timersPausedByUs = new java.util.LinkedHashSet<>();

    // ── Lifecycle tracking ─────────────────────────────────────────────────────
    // Tracks which fighter (per side) most recently received onBattleStart.
    // Prevents onBattleStart from re-firing for a fighter that is STILL
    // active, just because the OPPOSING side's fighter changed.
    private Character lastStartedPlayer = null;
    private Enemy      lastStartedEnemy  = null;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Single character vs single enemy — convenience constructor. */
    public Battle(Character player, Enemy enemy) {
        this.playerTeam = new ArrayList<>(List.of(player));
        this.enemyTeam  = new ArrayList<>(List.of(enemy));
    }

    /** Maximum number of characters allowed in the player's team (dojo limit). */
    public static final int MAX_PLAYER_TEAM_SIZE = 4;

    /** Full team vs team constructor. */
    public Battle(List<Character> playerTeam, List<Enemy> enemyTeam) {
        if (playerTeam.isEmpty() || enemyTeam.isEmpty())
            throw new IllegalArgumentException("Teams must not be empty.");
        if (playerTeam.size() > MAX_PLAYER_TEAM_SIZE)
            throw new IllegalArgumentException(
                    "Player team cannot exceed " + MAX_PLAYER_TEAM_SIZE + " characters.");
        long distinct = playerTeam.stream().distinct().count();
        if (distinct < playerTeam.size())
            throw new IllegalArgumentException(
                    "Player team cannot contain duplicate characters.");
        this.playerTeam = new ArrayList<>(playerTeam);
        this.enemyTeam  = new ArrayList<>(enemyTeam);
    }

    public void addListener(BattleListener l) { listeners.add(l); }

    // ── Active fighter accessors ──────────────────────────────────────────────

    public Character getActivePlayer() { return playerTeam.get(playerIndex); }
    public Enemy     getActiveEnemy()  { return enemyTeam.get(enemyIndex); }

    /** IBattle contract — returns the active fighters. */
    @Override public Entity getPlayer() { return getActivePlayer(); }
    @Override public Entity getEnemy()  { return getActiveEnemy();  }

    public List<Character> getPlayerTeam() { return playerTeam; }
    public List<Enemy>     getEnemyTeam()  { return enemyTeam;  }

    public int getPlayerIndex() { return playerIndex; }
    public int getEnemyIndex()  { return enemyIndex;  }

    /** Damage dealt by each player character, sorted descending by damage for display. */
    public java.util.List<java.util.Map.Entry<Character, Integer>> getDamageRanking() {
        return damageDealt.entrySet().stream()
                .sorted(java.util.Map.Entry.<Character, Integer>comparingByValue().reversed())
                .collect(java.util.stream.Collectors.toList());
    }

    /** Total enemy HP pool: base max HP + all healing received during the battle. */
    public int getTotalEnemyMaxHp() {
        return enemyHpPool;
    }

    /** How many player fighters are still waiting (not yet fought). */
    public int playerFightersRemaining() {
        return playerTeam.size() - playerIndex - 1;
    }

    /** How many enemy fighters are still waiting. */
    public int enemyFightersRemaining() {
        return enemyTeam.size() - enemyIndex - 1;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void start() {
        playerIndex = 0;
        enemyIndex  = 0;
        playerTeam.forEach(Character::reset);
        enemyTeam.forEach(Enemy::reset);
        damageDealt.clear();
        playerTeam.forEach(c -> damageDealt.put(c, 0));
        enemyHpPool = enemyTeam.stream().mapToInt(Enemy::getMaxHp).sum();
        lastStartedPlayer = null;
        lastStartedEnemy  = null;
        state = BattleState.APPROACHING;
    }

    /** Called by BattlePanel once both active fighters have reached the centre. */
    public void setEngaged() {
        if (state != BattleState.APPROACHING) return;
        state = BattleState.ONGOING;
        collectPassives();
        fireBattleStartForNewFighters();
        startTickTimers();
    }

    /** Stop all passive timers (called on battle end or reset). */
    public void stop() {
        stopPassiveTimers();
        pausableTimers.forEach(javax.swing.Timer::stop);
        pausableTimers.clear();
        timersPausedByUs.clear();
    }

    /**
     * Suspends all passive tick timers AND registered one-shot timers.
     * Tracks exactly which timers were running so resume() only restarts
     * those — avoids restarting a one-shot timer that already fired naturally.
     */
    public void pause() {
        timersPausedByUs.clear();
        for (javax.swing.Timer t : passiveTimers) {
            if (t.isRunning()) { t.stop(); timersPausedByUs.add(t); }
        }
        for (javax.swing.Timer t : pausableTimers) {
            if (t.isRunning()) { t.stop(); timersPausedByUs.add(t); }
        }
    }

    /**
     * Resumes only the timers that were running when pause() was called.
     * One-shot timers that had already fired before the pause are not restarted.
     */
    public void resume() {
        if (state != BattleState.ONGOING) return;
        timersPausedByUs.forEach(javax.swing.Timer::start);
        timersPausedByUs.clear();
    }

    /**
     * Registers a one-shot or lifecycle timer so pause()/resume() include it.
     * Call from a passive's onBattleStart when creating a timer that must
     * stop when the info overlay opens (e.g. Zayir's 2-second ATK boost timer).
     */
    @Override
    public void registerPausableTimer(javax.swing.Timer timer) {
        pausableTimers.add(timer);
    }

    /**
     * Removes a timer from the pausable registry — call when it has fired so
     * it doesn't accumulate across fighters or battles.
     */
    @Override
    public void unregisterPausableTimer(javax.swing.Timer timer) {
        pausableTimers.remove(timer);
        timersPausedByUs.remove(timer);
    }

    /** Temporarily suspend passive timers without clearing them (used by pause). */
    public void pausePassiveTimers() {
        passiveTimers.forEach(javax.swing.Timer::stop);
    }

    /** Resume suspended passive timers after a pause. */
    public void resumePassiveTimers() {
        passiveTimers.forEach(javax.swing.Timer::start);
    }

    // ── Attack ticks ──────────────────────────────────────────────────────────

    public void playerTick() {
        if (state != BattleState.ONGOING) return;
        Entity attacker = getActivePlayer();
        Entity defender = getActiveEnemy();

        DamageResult result = attacker.calculateDamage(defender);
        if (result.isMiss) {
            String log = attacker.getName() + "'s attack missed!";
            for (BattleListener l : listeners) l.onPlayerAttack(log, 0, false, true);
            return;
        }
        int dmg = applyPassiveEvent(defender, attacker, PassiveEvent.ON_TAKE_DAMAGE, result.amount, result.isCrit);
        applyPassiveEvent(attacker, defender, PassiveEvent.ON_DEAL_DAMAGE, dmg, result.isCrit);
        int actualDmg = Math.min(dmg, defender.getCurrentHp());   // cap overkill
        defender.takeDamage(dmg);
        trackDamage(getActivePlayer(), actualDmg);

        String log = (result.isCrit ? "★ CRIT! " : "")
                + attacker.getName() + " hits " + defender.getName()
                + " for " + dmg + " dmg! ("
                + defender.getCurrentHp() + "/" + defender.getMaxHp() + " HP)";
        for (BattleListener l : listeners) l.onPlayerAttack(log, dmg, result.isCrit, false);
        checkEnd();
    }

    public void enemyTick() {
        if (state != BattleState.ONGOING) return;
        Entity attacker = getActiveEnemy();
        Entity defender = getActivePlayer();

        DamageResult result = attacker.calculateDamage(defender);
        if (result.isMiss) {
            String log = attacker.getName() + "'s attack missed!";
            for (BattleListener l : listeners) l.onEnemyAttack(log, 0, false, true);
            return;
        }
        int dmg = applyPassiveEvent(defender, attacker, PassiveEvent.ON_TAKE_DAMAGE, result.amount, result.isCrit);
        applyPassiveEvent(attacker, defender, PassiveEvent.ON_DEAL_DAMAGE, dmg, result.isCrit);
        defender.takeDamage(dmg);

        String log = (result.isCrit ? "★ CRIT! " : "")
                + attacker.getName() + " hits " + defender.getName()
                + " for " + dmg + " dmg! ("
                + defender.getCurrentHp() + "/" + defender.getMaxHp() + " HP)";
        for (BattleListener l : listeners) l.onEnemyAttack(log, dmg, result.isCrit, false);
        checkEnd();
    }

    // ── IBattle: passive event dispatch ───────────────────────────────────────

    @Override
    public int applyPassiveEvent(Entity owner, Entity target,
                                 PassiveEvent event, int damage, boolean isCrit) {
        List<Passive> passives = (owner == getActivePlayer()) ? playerPassives : enemyPassives;
        PassiveContext ctx = new PassiveContext(owner, target, this, event, damage, isCrit);
        for (Passive p : passives) {
            if (p.respondsTo().contains(event)) p.trigger(ctx);
        }
        return ctx.incomingDamage;
    }

    // ── Passive management ────────────────────────────────────────────────────

    private void collectPassives() {
        playerPassives.clear();
        enemyPassives.clear();
        playerPassives.addAll(getActivePlayer().getActivePassives());
        Passive ep = getActiveEnemy().getPassive();
        if (ep != null) enemyPassives.add(ep);
    }

    /**
     * Fires onBattleStart for the CURRENTLY active player and/or enemy —
     * but ONLY for whichever side's active fighter differs from the last
     * fighter that already received onBattleStart this battle session.
     *
     * This is what stops passives like Lynx's "shield once per battle"
     * from re-triggering just because the OPPOSING side's fighter rotated
     * in (e.g. the player's next character entering after the previous
     * one died) — Lynx's own fighter never changed, so he should not be
     * treated as "newly entering" again.
     */
    private void fireBattleStartForNewFighters() {
        Character currentPlayer = getActivePlayer();
        if (currentPlayer != lastStartedPlayer) {
            for (Passive p : playerPassives) p.onBattleStart(currentPlayer, this);
            lastStartedPlayer = currentPlayer;
        }
        Enemy currentEnemy = getActiveEnemy();
        if (currentEnemy != lastStartedEnemy) {
            for (Passive p : enemyPassives) p.onBattleStart(currentEnemy, this);
            lastStartedEnemy = currentEnemy;
        }
    }

    private void startTickTimers() {
        for (Passive p : playerPassives) {
            if (p.respondsTo().contains(PassiveEvent.TICK) && p.getIntervalMs() > 0) {
                final Entity owner = getActivePlayer(), target = getActiveEnemy();
                javax.swing.Timer t = new javax.swing.Timer(p.getIntervalMs(), e ->
                        p.trigger(new PassiveContext(owner, target, this, PassiveEvent.TICK, 0, false)));
                t.start();
                passiveTimers.add(t);
            }
        }
        for (Passive p : enemyPassives) {
            if (p.respondsTo().contains(PassiveEvent.TICK) && p.getIntervalMs() > 0) {
                final Entity owner = getActiveEnemy(), target = getActivePlayer();
                javax.swing.Timer t = new javax.swing.Timer(p.getIntervalMs(), e ->
                        p.trigger(new PassiveContext(owner, target, this, PassiveEvent.TICK, 0, false)));
                t.start();
                passiveTimers.add(t);
            }
        }
    }

    private void stopPassiveTimers() {
        passiveTimers.forEach(javax.swing.Timer::stop);
        passiveTimers.clear();
    }

    // ── End / next fighter ────────────────────────────────────────────────────

    @Override
    public void checkEndPublic() { checkEnd(); }

    private void checkEnd() {
        if (!getActiveEnemy().isAlive()) {
            // The enemy's active fighter died — clean up ONLY its own passives.
            // The player's fighter is still alive and still fighting, so its
            // onBattleEnd must NOT fire here.
            for (Passive p : enemyPassives) p.onBattleEnd(getActiveEnemy(), this);
            stopPassiveTimers();
            if (enemyIndex + 1 < enemyTeam.size()) {
                // Next enemy enters
                enemyIndex++;
                int remaining = enemyFightersRemaining();
                for (BattleListener l : listeners)
                    l.onFighterEnter(false, getActiveEnemy(), remaining);
                // Panel will call setNextEngaged() once the approach animation completes
            } else {
                state = BattleState.PLAYER_WIN;
                // Battle is truly over — clean up the surviving player fighter's
                // passives too (e.g. permanent passive ATK bonuses) exactly once.
                for (Passive p : playerPassives) p.onBattleEnd(getActivePlayer(), this);
                for (BattleListener l : listeners) l.onBattleEnd(state);
            }
        } else if (!getActivePlayer().isAlive()) {
            for (Passive p : playerPassives) p.onBattleEnd(getActivePlayer(), this);
            stopPassiveTimers();
            if (playerIndex + 1 < playerTeam.size()) {
                // Next player fighter enters
                playerIndex++;
                int remaining = playerFightersRemaining();
                for (BattleListener l : listeners)
                    l.onFighterEnter(true, getActivePlayer(), remaining);
            } else {
                state = BattleState.ENEMY_WIN;
                for (Passive p : enemyPassives) p.onBattleEnd(getActiveEnemy(), this);
                for (BattleListener l : listeners) l.onBattleEnd(state);
            }
        }
    }

    /**
     * Called by BattlePanel after the new fighter's approach animation completes.
     * Resumes ONGOING state and starts the new fighter's passives.
     *
     * Only the side whose fighter actually changed gets onBattleStart fired —
     * see fireBattleStartForNewFighters().
     */
    public void setNextEngaged() {
        state = BattleState.ONGOING;
        collectPassives();
        fireBattleStartForNewFighters();
        startTickTimers();
    }

    // ── Damage / heal accounting ──────────────────────────────────────────────

    /**
     * Records damage dealt by a player Character against the enemy HP pool.
     * Called for both direct attacks and passive effects.
     */
    private void trackDamage(Character attacker, int amount) {
        if (amount > 0) damageDealt.merge(attacker, amount, Integer::sum);
    }

    // ── IBattle notifications ─────────────────────────────────────────────────

    @Override
    public void notifyShield(Entity owner, String passiveName, int amount) {
        // Build log with current shield total if available
        String total = (owner instanceof Shielded s)
                ? " (total: " + s.getShieldHp() + ")" : "";
        String log = "[" + passiveName + "] " + owner.getName()
                + " — +" + amount + " shield" + total;
        // isHeal=true for green popup; does NOT touch enemyHpPool
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
        // Track damage contribution the same way notifyPassive does for player characters
        if (amount > 0 && owner instanceof Character c) {
            trackDamage(c, amount);
        }
        String log = "[" + passiveName + "] " + (isCrit ? "★ CRIT! " : "")
                + owner.getName() + " — " + effectDesc
                + " (" + target.getName() + ": "
                + target.getCurrentHp() + "/" + target.getMaxHp() + " HP)";
        for (BattleListener l : listeners) l.onSpecialHit(log, owner, target, amount, isCrit);
    }

    @Override
    public void notifyPassive(Entity owner, Entity target, String passiveName,
                              String effectDesc, int amount, boolean isHeal) {
        if (amount > 0) {
            if (!isHeal && owner instanceof Character c) {
                // Player character dealt passive damage — count it
                trackDamage(c, amount);
            } else if (isHeal && !(owner instanceof Character)) {
                // Enemy healed — expand the HP pool so percentages stay accurate
                enemyHpPool += amount;
            }
        }
        String log = "[" + passiveName + "] " + owner.getName() + " — " + effectDesc
                + " (" + target.getName() + ": "
                + target.getCurrentHp() + "/" + target.getMaxHp() + " HP)";
        for (BattleListener l : listeners) l.onPassive(log, owner, amount, isHeal);
    }

    public BattleState getState() { return state; }
}