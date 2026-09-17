package Combat;

import Combat.NormalBattle.*;
import Entities.Character;
import Entities.Characters.*;
import Entities.Bosses.*;
import Entities.Enemy;
import GameMain.GamePanel;
import GameModes.TowerOfSuffering.FloorMode;

import javax.swing.*;
import java.util.List;

//public class BattleMainTester {
//
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            GamePanel window = new GamePanel();
//
//            // Character vs Enemy test battle
//            List<Character> team = List.of(new Zed(), new Kaizen());
//            List<Enemy> enemies  = List.of(new Phainon());
//            window.showPanel(new BattlePanel(new Battle(team, enemies)));
//
//            // Character vs Character test battle
//            //CharacterBattle battle = new CharacterBattle(new Kaizen(), new Zed()); window.showPanel(new CharacterBattlePanel(battle));
//        });
//    }
//}

public class BattleMainTester {

    // Delay (ms) after battle ends before the next floor starts
    private static final int NEXT_FLOOR_DELAY_MS = 3000;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel window = new GamePanel();

            // ── Build player team ─────────────────────────────────────────────
            Zed    zed    = new Zed();
            Kaizen kaizen = new Kaizen();
            List<Entities.Character> team = List.of(zed, kaizen);

            // ── Create FloorMode ──────────────────────────────────────────────
            FloorMode mode = new FloorMode(team);

            // ── Wire FloorMode listener (for logging / future UI hooks) ───────
            mode.addListener(new FloorMode.FloorModeListener() {
                @Override
                public void onFloorStart(GameModes.TowerOfSuffering.Floor floor) {
                    System.out.println("▶ Starting " + floor
                            + (floor.isBoss() ? " ★ BOSS FLOOR" : ""));
                }

                @Override
                public void onFloorComplete(int floorNumber, boolean wasBoss) {
                    System.out.println("✔ Floor " + floorNumber + " cleared"
                            + (wasBoss ? " [BOSS]" : "") + "!");
                }

                @Override
                public void onModeComplete() {
                    System.out.println("🏆 All defined floors cleared — Tower test complete!");
                }

                @Override
                public void onGameOver(int lastFloor) {
                    System.out.println("💀 Game Over on floor " + lastFloor + ".");
                }
            });

            // ── Start first floor ─────────────────────────────────────────────
            startNextFloor(window, mode);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a Battle for the current floor, shows the BattlePanel,
     * and attaches a listener that advances FloorMode when the battle ends.
     */
    private static void startNextFloor(GamePanel window, FloorMode mode) {

        // Guard: stop if all defined floors are done or mode is inactive
        if (!mode.isActive()) {
            System.out.println("FloorMode is no longer active (state: " + mode.getState() + ").");
            return;
        }

        // Guard: floors 11+ have no enemies yet — stop cleanly
        int floorNum = mode.getCurrentFloorNumber();
        if (floorNum > 10) {
            System.out.println("Floors 11+ have no enemies yet. Test ends at floor 10.");
            return;
        }

        Battle battle = mode.startCurrentFloor();

        // Attach our advancement listener BEFORE showing the panel
        // (BattlePanel also adds its own listener — multiple listeners are fine)
        battle.addListener(new Battle.BattleListener() {
            @Override
            public void onBattleEnd(Battle.BattleState result) {
                SwingUtilities.invokeLater(() -> {
                    if (result == Battle.BattleState.PLAYER_WIN) {
                        mode.onBattleWon();
                        // Wait for victory screen, then load next floor
                        if (mode.getState() != FloorMode.State.COMPLETE) {
                            new javax.swing.Timer(NEXT_FLOOR_DELAY_MS, e -> {
                                ((javax.swing.Timer) e.getSource()).stop();
                                startNextFloor(window, mode);
                            }) {{ setRepeats(false); start(); }};
                        }
                    } else {
                        mode.onBattleLost();
                    }
                });
            }

            // ── Unused listener stubs ─────────────────────────────────────────
            @Override public void onPlayerAttack(String log, int dmg, boolean crit, boolean miss) {}
            @Override public void onEnemyAttack (String log, int dmg, boolean crit, boolean miss) {}
            @Override public void onPassive(String log, Entities.Entity owner, int amount, boolean heal) {}
            @Override public void onFighterEnter(boolean isPlayer, Entities.Entity fighter, int remaining) {}
        });

        window.showPanel(new BattlePanel(battle));
        System.out.println("Showing floor " + floorNum);
    }
}
