package GameModes.TowerOfSuffering;

import Combat.NormalBattle.Battle;
import Entities.Character;
import Entities.Bosses.*;
import Entities.Enemies.*;
import Entities.Enemy;

import java.util.ArrayList;
import java.util.List;

import Economy.Currency;
import Economy.FloorRewardTable;
import Economy.Wallet;

import java.util.ArrayList;
import java.util.List;

/**
 * FloorMode — logic layer for the floor-climbing game mode.
 *
 * The player climbs 100 floors fighting enemies. Every 5th floor is a boss.
 * Character HP carries over between floors — there is no rest between fights.
 *
 * ── How to connect to UI later ─────────────────────────────────────────────
 *
 *  1. Construct:   FloorMode mode = new FloorMode(playerTeam);
 *  2. Subscribe:   mode.addListener(yourFloorModeListener);
 *  3. Start:       Battle battle = mode.startCurrentFloor();
 *  4. Wire battle: battle.addListener(yourBattleListener);
 *                  yourGamePanel.showPanel(new BattlePanel(battle));
 *  5. On win:      call mode.onBattleWon()  → listener fires onFloorComplete / onModeComplete
 *  6. On loss:     call mode.onBattleLost() → listener fires onGameOver
 *
 *  The BattlePanel already calls battle.stop() on end; FloorMode does not
 *  need to manage Battle internals — only its own state.
 *
 * ── Floor layout ───────────────────────────────────────────────────────────
 *
 *  Floors  1– 4  : normal
 *  Floor   5     : BOSS
 *  Floors  6– 9  : normal
 *  Floor  10     : BOSS
 *  … (pattern repeats every 5 floors up to 100)
 *
 * ── Package suggestion ─────────────────────────────────────────────────────
 *  Place in: src/GameModes/FloorMode.java
 *  Add at top: package GameModes;
 *  Then add imports for Combat.NormalBattle.Battle,
 *               Entities.Character, Entities.Enemy,
 *               Entities.Enemies.Phainon, Entities.Enemies.Hanzo, etc.
 */
public class FloorMode {

    // ── State ─────────────────────────────────────────────────────────────────

    public enum State {
        IDLE,           // not started yet
        IN_BATTLE,      // battle is currently running
        FLOOR_COMPLETE, // floor won, awaiting advance to next floor
        GAME_OVER,      // player lost — all characters defeated
        COMPLETE        // all 100 floors cleared
    }

    // ── Listener interface (UI hooks) ─────────────────────────────────────────

    /**
     * Implement this in your UI layer to react to floor mode events.
     * Register via addListener().
     */
    public interface FloorModeListener {
        /**
         * Fired when the player is about to start a floor.
         * Use this to display the floor title card / transition screen.
         */
        void onFloorStart(Floor floor);

        /**
         * Fired after the player wins a floor.
         * Show a "floor cleared" screen or transition before calling startCurrentFloor() again.
         */
        void onFloorComplete(int floorNumber, boolean wasBoss);

        /**
         * Fired right after onFloorComplete with the Gold/Elixir granted for
         * this floor (see Economy.FloorRewardTable). Use this to show a
         * "+50 Gold  +10 Elixir" popup on the floor-cleared screen.
         */
        void onFloorReward(int gold, int elixir);

        /**
         * Fired when the player clears all 100 floors.
         */
        void onModeComplete();

        /**
         * Fired when all player characters have been defeated.
         * @param lastFloor  the floor number on which they fell
         */
        void onGameOver(int lastFloor);
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private final List<Character>       playerTeam;
    private final List<Floor>           floors;
    private       int                   currentFloorIndex = 0;
    private       State                 state             = State.IDLE;
    private final List<FloorModeListener> listeners       = new ArrayList<>();
    private final Wallet                 wallet;

    /** Maximum team size enforced here as a safety guard (mirrors Battle). */
    public static final int MAX_TEAM_SIZE = Battle.MAX_PLAYER_TEAM_SIZE;

    // ── Constructor ───────────────────────────────────────────────────────────
    public FloorMode(List<Character> playerTeam, Wallet wallet) {
        if (playerTeam == null || playerTeam.isEmpty())
            throw new IllegalArgumentException("Player team cannot be empty.");
        if (playerTeam.size() > MAX_TEAM_SIZE)
            throw new IllegalArgumentException("Player team cannot exceed " + MAX_TEAM_SIZE + " characters.");
        long distinct = playerTeam.stream().distinct().count();
        if (distinct < playerTeam.size())
            throw new IllegalArgumentException("Player team cannot contain duplicate characters.");
        if (wallet == null)
            throw new IllegalArgumentException("Wallet cannot be null.");

        this.playerTeam = new ArrayList<>(playerTeam);
        this.floors     = buildFloors();
        this.wallet     = wallet;
    }

    /** The player's wallet — also accessible directly if you constructed it externally. */
    public Wallet getWallet() { return wallet; }

    // ── Listener management ───────────────────────────────────────────────────

    public void addListener(FloorModeListener l)    { listeners.add(l); }
    public void removeListener(FloorModeListener l) { listeners.remove(l); }

    // ── Core API ──────────────────────────────────────────────────────────────

    /**
     * Creates and returns a Battle for the current floor.
     * The caller should add a Battle.BattleListener to detect win/loss and then call
     * onBattleWon() or onBattleLost() on this FloorMode accordingly.
     *
     * Player character HP is NOT reset — it carries over from the previous floor.
     */
    public Battle startCurrentFloor() {
        if (state == State.COMPLETE || state == State.GAME_OVER)
            throw new IllegalStateException("FloorMode is no longer active.");

        state = State.IN_BATTLE;
        Floor floor = getCurrentFloor();
        for (FloorModeListener l : listeners) l.onFloorStart(floor);

        List<Enemy> enemies = floor.createEnemies();
        return new Battle(playerTeam, enemies);
    }

    /**
     * Call this when the player wins the current floor's battle.
     * Advances to the next floor and fires the appropriate listener events.
     */
    public void onBattleWon() {
        if (state != State.IN_BATTLE) return;

        int completedFloor = getCurrentFloor().getFloorNumber();
        boolean wasBoss    = getCurrentFloor().isBoss();
        state = State.FLOOR_COMPLETE;

        for (FloorModeListener l : listeners) l.onFloorComplete(completedFloor, wasBoss);

        // ── Grant Gold/Elixir reward for clearing this floor ──────────────────
        FloorRewardTable.Reward reward = FloorRewardTable.getReward(completedFloor);
        if (reward.gold() > 0)   wallet.add(Currency.GOLD,   reward.gold());
        if (reward.elixir() > 0) wallet.add(Currency.ELIXIR, reward.elixir());
        for (FloorModeListener l : listeners) l.onFloorReward(reward.gold(), reward.elixir());

        currentFloorIndex++;
        if (currentFloorIndex >= floors.size()) {
            state = State.COMPLETE;
            for (FloorModeListener l : listeners) l.onModeComplete();
        }
    }

    /**
     * Call this when the player loses the current floor's battle.
     */
    public void onBattleLost() {
        if (state != State.IN_BATTLE) return;
        state = State.GAME_OVER;
        int lastFloor = getCurrentFloor().getFloorNumber();
        for (FloorModeListener l : listeners) l.onGameOver(lastFloor);
    }

    // ── State queries ─────────────────────────────────────────────────────────

    public Floor  getCurrentFloor()       { return floors.get(Math.min(currentFloorIndex, floors.size() - 1)); }
    public int    getCurrentFloorNumber() { return getCurrentFloor().getFloorNumber(); }
    public int    getTotalFloors()        { return floors.size(); }
    public State  getState()              { return state; }
    public boolean isActive()            { return state != State.COMPLETE && state != State.GAME_OVER; }

    /** How many floors have been cleared so far. */
    public int getFloorsCleared() { return currentFloorIndex; }

    // ─────────────────────────────────────────────────────────────────────────
    //  Floor definitions (1–100)
    // ─────────────────────────────────────────────────────────────────────────
    private List<Floor> buildFloors() {
        List<Floor> f = new ArrayList<>();

        // ── Floors 1–4 ────────────────────────────────────────────────────────
        f.add(new Floor(1, () -> List.of(new UnknownSubject(1))));
        f.add(new Floor(2, () -> List.of(new ExperimentalSubject(1))));
        f.add(new Floor(3, () -> List.of(new UnknownSubject(1), new UnknownSubject(1))));
        f.add(new Floor(4, () -> List.of(new  ExperimentalSubject(1),  new ExperimentalSubject(1))));

        // ── Floor 5 — ★ BOSS ─────────────────────────────────────────────────
        f.add(new Floor(5, () -> List.of(new Phainon())));

        // ── Floors 6–9 ────────────────────────────────────────────────────────
        f.add(new Floor(6, () -> List.of(new ExperimentalSubject(1), new UnknownSubject(1))));
        f.add(new Floor(7, () -> List.of(new UnknownSubject(1), new UnknownSubject(1), new UnknownSubject(1))));
        f.add(new Floor(8, () -> List.of(new ExperimentalSubject(1), new ExperimentalSubject(1), new ExperimentalSubject(1))));
        f.add(new Floor(9, () -> List.of(new  UnknownSubject(1), new ExperimentalSubject(1), new UnknownSubject(1), new ExperimentalSubject(1))));

        // ── Floor 10 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(10, () -> List.of(new Hanzo())));

        // ── Floors 11–14 ──────────────────────────────────────────────────────
        f.add(new Floor(11, () -> List.of(new IndestructibleSubject(1), new IndestructibleSubject(1))));
        f.add(new Floor(12, () -> List.of(new IndestructibleSubject(2))));
        f.add(new Floor(13, () -> List.of(new UnknownSubject(2), new ExperimentalSubject(2))));
        f.add(new Floor(14, () -> List.of(new IndestructibleSubject(2), new UnknownSubject(2), new ExperimentalSubject(2))));

        // ── Floor 15 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(15, () -> List.of(new Lynx())));

        // ── Floors 16–19 ──────────────────────────────────────────────────────
        f.add(new Floor(16, () -> List.of(new IndestructibleSubject(3))));
        f.add(new Floor(17, () -> List.of(new UnknownSubject(3), new ExperimentalSubject(3))));
        f.add(new Floor(18, () -> List.of(new IndestructibleSubject(4), new UnknownSubject(4))));
        f.add(new Floor(19, () -> List.of(new ExperimentalSubject(4), new IndestructibleSubject(4))));

        // ── Floor 20 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(20, () -> List.of(new Shogun())));

        // ── Floors 21–24 ──────────────────────────────────────────────────────
        f.add(new Floor(21, () -> List.of(new UnknownSubject(5), new ExperimentalSubject(5))));
        f.add(new Floor(22, () -> List.of(new IndestructibleSubject(5), new IndestructibleSubject(5))));
        f.add(new Floor(23, () -> List.of(new IndestructibleSubject(6))));
        f.add(new Floor(24, () -> List.of(new  UnknownSubject(6), new IndestructibleSubject(6), new ExperimentalSubject(6))));

        // ── Floor 25 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(25, () -> List.of(new Scar())));

        // ── Floors 26–29 ──────────────────────────────────────────────────────
        f.add(new Floor(26, () -> List.of(new UnknownSubject(7))));
        f.add(new Floor(27, () -> List.of(new ExperimentalSubject(7))));
        f.add(new Floor(28, () -> List.of(new IndestructibleSubject(7))));
        f.add(new Floor(29, () -> List.of(new UnknownSubject(7), new IndestructibleSubject(7), new ExperimentalSubject(7))));

        // ── Floor 30 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(30, () -> List.of(new Stain())));

        // ── Floors 31–34 ──────────────────────────────────────────────────────
        f.add(new Floor(31, () -> List.of(new ExperimentalSubject(8))));
        f.add(new Floor(32, () -> List.of(new IndestructibleSubject(8))));
        f.add(new Floor(33, () -> List.of(new UnknownSubject(8))));
        f.add(new Floor(34, () -> List.of(new UnknownSubject(8), new IndestructibleSubject(8), new ExperimentalSubject(8))));

        // ── Floor 35 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(35, () -> List.of(new Groon())));

        // ── Floors 36–39 ──────────────────────────────────────────────────────
        f.add(new Floor(36, () -> List.of(new UnknownSubject(8), new ExperimentalSubject(8))));
        f.add(new Floor(37, () -> List.of(new IndestructibleSubject(8), new IndestructibleSubject(8))));
        f.add(new Floor(38, () -> List.of(new ExperimentalSubject(9))));
        f.add(new Floor(39, () -> List.of(new UnknownSubject(9))));

        // ── Floor 40 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(40, () -> List.of(new Gouto())));

        // ── Floors 41–44 ──────────────────────────────────────────────────────
        f.add(new Floor(41, () -> List.of(new IndestructibleSubject(9))));
        f.add(new Floor(42, () -> List.of(new IndestructibleSubject(9),  new UnknownSubject(9))));
        f.add(new Floor(43, () -> List.of(new ExperimentalSubject(9), new IndestructibleSubject(9))));
        f.add(new Floor(44, () -> List.of(new ExperimentalSubject(9), new IndestructibleSubject(9),   new UnknownSubject(9))));

        // ── Floor 45 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(45, () -> List.of(new Maki())));

        // ── Floors 46–49 ──────────────────────────────────────────────────────
        f.add(new Floor(46, () -> List.of(new ExperimentalSubject(10), new ExperimentalSubject(10))));
        f.add(new Floor(47, () -> List.of(new UnknownSubject(10),  new IndestructibleSubject(10))));
        f.add(new Floor(48, () -> List.of(new IndestructibleSubject(10), new ExperimentalSubject(10))));
        f.add(new Floor(49, () -> List.of(new UnknownSubject(10), new IndestructibleSubject(10),   new ExperimentalSubject(10))));

        // ── Floor 50 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(50, () -> List.of( /* TODO: add boss enemies for floor 50 */ )));

        // ── Floors 51–54 ──────────────────────────────────────────────────────
        f.add(new Floor(51, () -> List.of( /* TODO: add enemies for floor 51 */ )));
        f.add(new Floor(52, () -> List.of( /* TODO: add enemies for floor 52 */ )));
        f.add(new Floor(53, () -> List.of( /* TODO: add enemies for floor 53 */ )));
        f.add(new Floor(54, () -> List.of( /* TODO: add enemies for floor 54 */ )));

        // ── Floor 55 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(55, () -> List.of( /* TODO: add boss enemies for floor 55 */ )));

        // ── Floors 56–59 ──────────────────────────────────────────────────────
        f.add(new Floor(56, () -> List.of( /* TODO: add enemies for floor 56 */ )));
        f.add(new Floor(57, () -> List.of( /* TODO: add enemies for floor 57 */ )));
        f.add(new Floor(58, () -> List.of( /* TODO: add enemies for floor 58 */ )));
        f.add(new Floor(59, () -> List.of( /* TODO: add enemies for floor 59 */ )));

        // ── Floor 60 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(60, () -> List.of( /* TODO: add boss enemies for floor 60 */ )));

        // ── Floors 61–64 ──────────────────────────────────────────────────────
        f.add(new Floor(61, () -> List.of( /* TODO: add enemies for floor 61 */ )));
        f.add(new Floor(62, () -> List.of( /* TODO: add enemies for floor 62 */ )));
        f.add(new Floor(63, () -> List.of( /* TODO: add enemies for floor 63 */ )));
        f.add(new Floor(64, () -> List.of( /* TODO: add enemies for floor 64 */ )));

        // ── Floor 65 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(65, () -> List.of( /* TODO: add boss enemies for floor 65 */ )));

        // ── Floors 66–69 ──────────────────────────────────────────────────────
        f.add(new Floor(66, () -> List.of( /* TODO: add enemies for floor 66 */ )));
        f.add(new Floor(67, () -> List.of( /* TODO: add enemies for floor 67 */ )));
        f.add(new Floor(68, () -> List.of( /* TODO: add enemies for floor 68 */ )));
        f.add(new Floor(69, () -> List.of( /* TODO: add enemies for floor 69 */ )));

        // ── Floor 70 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(70, () -> List.of( /* TODO: add boss enemies for floor 70 */ )));

        // ── Floors 71–74 ──────────────────────────────────────────────────────
        f.add(new Floor(71, () -> List.of( /* TODO: add enemies for floor 71 */ )));
        f.add(new Floor(72, () -> List.of( /* TODO: add enemies for floor 72 */ )));
        f.add(new Floor(73, () -> List.of( /* TODO: add enemies for floor 73 */ )));
        f.add(new Floor(74, () -> List.of( /* TODO: add enemies for floor 74 */ )));

        // ── Floor 75 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(75, () -> List.of( /* TODO: add boss enemies for floor 75 */ )));

        // ── Floors 76–79 ──────────────────────────────────────────────────────
        f.add(new Floor(76, () -> List.of( /* TODO: add enemies for floor 76 */ )));
        f.add(new Floor(77, () -> List.of( /* TODO: add enemies for floor 77 */ )));
        f.add(new Floor(78, () -> List.of( /* TODO: add enemies for floor 78 */ )));
        f.add(new Floor(79, () -> List.of( /* TODO: add enemies for floor 79 */ )));

        // ── Floor 80 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(80, () -> List.of( /* TODO: add boss enemies for floor 80 */ )));

        // ── Floors 81–84 ──────────────────────────────────────────────────────
        f.add(new Floor(81, () -> List.of( /* TODO: add enemies for floor 81 */ )));
        f.add(new Floor(82, () -> List.of( /* TODO: add enemies for floor 82 */ )));
        f.add(new Floor(83, () -> List.of( /* TODO: add enemies for floor 83 */ )));
        f.add(new Floor(84, () -> List.of( /* TODO: add enemies for floor 84 */ )));

        // ── Floor 85 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(85, () -> List.of( /* TODO: add boss enemies for floor 85 */ )));

        // ── Floors 86–89 ──────────────────────────────────────────────────────
        f.add(new Floor(86, () -> List.of( /* TODO: add enemies for floor 86 */ )));
        f.add(new Floor(87, () -> List.of( /* TODO: add enemies for floor 87 */ )));
        f.add(new Floor(88, () -> List.of( /* TODO: add enemies for floor 88 */ )));
        f.add(new Floor(89, () -> List.of( /* TODO: add enemies for floor 89 */ )));

        // ── Floor 90 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(90, () -> List.of( /* TODO: add boss enemies for floor 90 */ )));

        // ── Floors 91–94 ──────────────────────────────────────────────────────
        f.add(new Floor(91, () -> List.of( /* TODO: add enemies for floor 91 */ )));
        f.add(new Floor(92, () -> List.of( /* TODO: add enemies for floor 92 */ )));
        f.add(new Floor(93, () -> List.of( /* TODO: add enemies for floor 93 */ )));
        f.add(new Floor(94, () -> List.of( /* TODO: add enemies for floor 94 */ )));

        // ── Floor 95 — ★ BOSS ────────────────────────────────────────────────
        f.add(new Floor(95, () -> List.of( /* TODO: add boss enemies for floor 95 */ )));

        // ── Floors 96–99 ──────────────────────────────────────────────────────
        f.add(new Floor(96, () -> List.of( /* TODO: add enemies for floor 96 */ )));
        f.add(new Floor(97, () -> List.of( /* TODO: add enemies for floor 97 */ )));
        f.add(new Floor(98, () -> List.of( /* TODO: add enemies for floor 98 */ )));
        f.add(new Floor(99, () -> List.of( /* TODO: add enemies for floor 99 */ )));

        // ── Floor 100 — ★ FINAL BOSS ─────────────────────────────────────────
        f.add(new Floor(100, () -> List.of( /* TODO: add final boss enemies for floor 100 */ )));

        return f;
    }
}