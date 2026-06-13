package Combat;

import Combat.NormalBattle.*;
import Entities.Character;
import Entities.Characters.*;
import Entities.Bosses.*;
import Entities.Enemies.IndestructibleSubject;
import Entities.Enemy;
import Entities.Weapons.*;
import GameMain.GamePanel;
import GameModes.TowerOfSuffering.*;

import javax.swing.*;
import java.util.List;

//public class BattleMainTester {
//
//    public static void main(String[] args) {
//        SwingUtilities.invokeLater(() -> {
//            GamePanel window = new GamePanel();
//
//            // Character vs Enemy test battle
//            Zed zed = new Zed();
//            Kaizen kaizen = new Kaizen();
//            Zayir zayir = new Zayir();
//            List<Entities.Character> team = List.of(zayir, zed, kaizen);
//
//            Weapon weaponZ = new IronEdge();
//            Weapon weaponK = new IronEdge();
//            Weapon weapon  = new WolvesGravestone();
//
//            weaponZ.equip(zed); weaponZ.setWeaponLevel(10);
//            weaponK.equip(kaizen); weaponK.setWeaponLevel(10);
//            weapon.equip(zayir); weapon.setWeaponLevel(20);
//
//            List<Enemy> enemies  = List.of(new IndestructibleSubject(10));
//            window.showPanel(new BattlePanel(new Battle(team, enemies)));
//
//            // Character vs Character test battle
//            //CharacterBattle battle = new CharacterBattle(new Kaizen(), new Zed()); window.showPanel(new CharacterBattlePanel(battle));
//        });
//    }
//}

public class BattleMainTester {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel window = new GamePanel();

            // ── Build player team ─────────────────────────────────────────────
            Zed zed = new Zed();
            Kaizen kaizen = new Kaizen();
            Zayir zayir = new Zayir();
            List<Entities.Character> team = List.of(zayir, zed, kaizen);

            // ── Weapons ───────────────────────────────────────────────────────
            Weapon weaponZ = new IronEdge();
            Weapon weaponK = new IronEdge();
            Weapon weapon  = new WolvesGravestone();

            weaponZ.equip(zed); weaponZ.setWeaponLevel(10);
            weaponK.equip(kaizen); weaponK.setWeaponLevel(10);
            weapon.equip(zayir); weapon.setWeaponLevel(1);

            // ── Create FloorMode ──────────────────────────────────────────────
            FloorMode mode = new FloorMode(team);

            mode.addListener(new FloorMode.FloorModeListener() {
                @Override public void onFloorStart(Floor floor) {
                    System.out.println("▶ " + floor + (floor.isBoss() ? " ★ BOSS" : ""));
                }
                @Override public void onFloorComplete(int floorNumber, boolean wasBoss) {
                    System.out.println("✔ Floor " + floorNumber + " cleared" + (wasBoss ? " [BOSS]" : "") + "!");
                }
                @Override public void onModeComplete() {
                    System.out.println("🏆 All defined floors cleared!");
                }
                @Override public void onGameOver(int lastFloor) {
                    System.out.println("💀 Game Over on floor " + lastFloor + ".");
                }
            });

            // ── Start first floor ─────────────────────────────────────────────
            loadFloor(window, mode);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void loadFloor(GamePanel window, FloorMode mode) {
        // Stop cleanly if mode ended
        if (!mode.isActive()) {
            System.out.println("Run ended — state: " + mode.getState());
            return;
        }

        // Guard: floors 11+ have no enemies yet
        if (mode.getCurrentFloorNumber() > 14) {
            System.out.println("Floors 14+ not yet populated. Test ends here.");
            return;
        }

        Floor  floor  = mode.getCurrentFloor();
        Battle battle = mode.startCurrentFloor();

        // TowerBattleListener — wires the victory-screen buttons back into FloorMode
        TowerBattlePanel.TowerBattleListener towerListener = new TowerBattlePanel.TowerBattleListener() {
            @Override
            public void onNextFloor() {
                // Advance FloorMode, then load the next floor
                mode.onBattleWon();
                if (mode.getState() != FloorMode.State.COMPLETE) {
                    loadFloor(window, mode);
                } else {
                    System.out.println("All floors complete!");
                }
            }

            @Override
            public void onExit() {
                System.out.println("Player chose to exit.");
                // TODO: navigate back to title screen
                // GamePanel.get().showPanel(new TitleScreen());
                System.exit(0);
            }
        };

        // Attach a Battle listener just to detect defeat (TowerBattlePanel handles win via buttons)
        battle.addListener(new Battle.BattleListener() {
            @Override
            public void onBattleEnd(Battle.BattleState result) {
                if (result == Battle.BattleState.ENEMY_WIN) {
                    SwingUtilities.invokeLater(() -> mode.onBattleLost());
                }
            }
            @Override public void onPlayerAttack(String log, int dmg, boolean crit, boolean miss) {}
            @Override public void onEnemyAttack (String log, int dmg, boolean crit, boolean miss) {}
            @Override public void onPassive(String log, Entities.Entity owner, int amt, boolean heal) {}
            @Override public void onFighterEnter(boolean isPlayer, Entities.Entity fighter, int remaining) {}
        });

        window.showPanel(new TowerBattlePanel(battle, floor, towerListener));
        System.out.println("Loaded: " + floor);
    }
}
