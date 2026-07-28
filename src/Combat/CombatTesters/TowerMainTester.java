package Combat.CombatTesters;

import Combat.NormalBattle.Battle;
import Economy.Wallet;
import Entities.Artifacts.Artifact;
import Entities.Character;
import Entities.Characters.*;
import Entities.Weapons.*;
import GameMain.GamePanel;
import GameModes.TowerOfSuffering.Floor;
import GameModes.TowerOfSuffering.FloorMode;
import GameModes.TowerOfSuffering.TowerBattlePanel;

import javax.swing.*;
import java.util.List;

public class TowerMainTester {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel window = new GamePanel();

            // ── Build player team ─────────────────────────────────────────────
            Kindle kindle = new Kindle();
            Xyniz xyniz = new Xyniz();
            Zayir zayir = new Zayir();
            Zenzenkoi zenzenkoi = new Zenzenkoi();
            Kouzen kouzen = new Kouzen();
            List<Character> team = List.of(zayir, kouzen, xyniz, kindle);

            // ── Weapons ───────────────────────────────────────────────────────
            Weapon staff = new Staff();
            Weapon shield = new Shield();
            Weapon weaponZe = new Sword();
            Weapon weaponZ = new Saber();
            Weapon weaponK = new Saber();
            Weapon weapon  = new Sword();

            shield.equip(kouzen); shield.setWeaponLevel(20);
            weaponZe.equip(zenzenkoi); weaponZe.setWeaponLevel(20);
            weaponZ.equip(kindle); weaponZ.setWeaponLevel(20);
            weaponK.equip(xyniz); weaponK.setWeaponLevel(20);
            weapon.equip(zayir); weapon.setWeaponLevel(20);

            // ── Artifacts ─────────────────────────────────────────────────────
            Artifact a1 = Artifact.generateRandom();
            Artifact a2 = Artifact.generateRandom();
            Artifact a3 = Artifact.generateRandom();
            Artifact a4 = Artifact.generateRandom();

            Artifact b1 = Artifact.generateRandom();
            Artifact b2 = Artifact.generateRandom();
            Artifact b3 = Artifact.generateRandom();
            Artifact b4 = Artifact.generateRandom();

            kouzen.equipArtifact(0, a1);
            kouzen.equipArtifact(1, a2);
            kouzen.equipArtifact(2, a3);
            kouzen.equipArtifact(3, a4);

            zayir.equipArtifact(0, b1);
            zayir.equipArtifact(1, b2);
            zayir.equipArtifact(2, b3);
            zayir.equipArtifact(3, b4);

            System.out.println(zenzenkoi.getSummary());

            // ── Player wallet — receives floor rewards as floors are cleared ──
            Wallet wallet = new Wallet();

            // ── Create FloorMode ──────────────────────────────────────────────
            FloorMode mode = new FloorMode(team, wallet);

            mode.addListener(new FloorMode.FloorModeListener() {
                @Override public void onFloorStart(Floor floor) {
                    System.out.println("▶ " + floor + (floor.isBoss() ? " ★ BOSS" : ""));
                }
                @Override public void onFloorComplete(int floorNumber, boolean wasBoss) {
                    System.out.println("✔ Floor " + floorNumber + " cleared" + (wasBoss ? " [BOSS]" : "") + "!");
                }
                @Override public void onFloorReward(int gold, int elixir) {
                    System.out.println("    +" + gold + " Gold   +" + elixir + " Elixir   (Wallet: " + wallet + ")");
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
        if (mode.getCurrentFloorNumber() > 29) {
            System.out.println("Floors 30+ not yet populated. Test ends here.");
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
            @Override public void onPassiveMiss(String log, Entities.Entity owner, Entities.Entity target, String passiveName) {}
            @Override public void onSpecialHit(String log, Entities.Entity owner, Entities.Entity target, int amount, boolean isCrit) {}
            @Override public void onImmune(String log, Entities.Entity owner, Entities.Entity target, String passiveName) {}
            @Override public void onFighterEnter(boolean isPlayer, Entities.Entity fighter, int remaining) {}
        });

        window.showPanel(new TowerBattlePanel(battle, floor, towerListener));
        System.out.println("Loaded: " + floor);
    }
}