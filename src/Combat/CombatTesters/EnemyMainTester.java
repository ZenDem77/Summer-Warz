package Combat.CombatTesters;

import Combat.NormalBattle.Battle;
import Combat.NormalBattle.BattlePanel;
import Entities.Artifacts.Artifact;
import Entities.Bosses.*;
import Entities.Character;
import Entities.Characters.*;
import Entities.Enemies.*;
import Entities.Enemy;
import Entities.Weapons.IronEdge;
import Entities.Weapons.Weapon;
import Entities.Weapons.WolvesGravestone;
import GameMain.GamePanel;

import javax.swing.*;
import java.util.List;

public class EnemyMainTester {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel window = new GamePanel();

            // ═══════════════════════════════════════════════════════════════════
            //  PLAYER TEAM (1–4 characters, no duplicates)
            // ═══════════════════════════════════════════════════════════════════
            Zed zed = new Zed();
            Kaizen kaizen = new Kaizen();
            Zayir zayir = new Zayir();
            List<Character> team = List.of(kaizen, zed, zayir);

            Weapon weaponZ = new IronEdge();
            Weapon weaponK = new IronEdge();
            Weapon weapon  = new WolvesGravestone();

            weaponZ.equip(zed); weaponZ.setWeaponLevel(10);
            weaponK.equip(kaizen); weaponK.setWeaponLevel(10);
            weapon.equip(zayir); weapon.setWeaponLevel(10);

            Artifact a1 = Artifact.generateRandom();
            Artifact a2 = Artifact.generateRandom();
            Artifact a3 = Artifact.generateRandom();
            Artifact a4 = Artifact.generateRandom();

            kaizen.equipArtifact(0, a1);
            kaizen.equipArtifact(1, a2);
            kaizen.equipArtifact(2, a3);
            kaizen.equipArtifact(3, a4);

            // ═══════════════════════════════════════════════════════════════════
            //  ENEMY
            // ═══════════════════════════════════════════════════════════════════
            Enemy enemy = new Shogun();

            // ─────────────────────────────────────────────────────────────────
            Battle battle = new Battle(team, List.of(enemy));

            battle.addListener(new Battle.BattleListener() {
                @Override
                public void onBattleEnd(Battle.BattleState result) {
                    System.out.println(result == Battle.BattleState.PLAYER_WIN
                            ? "🏆 Player team wins!"
                            : "💀 " + enemy.getName() + " wins!");
                }
                @Override public void onPlayerAttack(String log, int dmg, boolean crit, boolean miss) {}
                @Override public void onEnemyAttack (String log, int dmg, boolean crit, boolean miss) {}
                @Override public void onPassive(String log, Entities.Entity owner, int amt, boolean heal) {}
                @Override public void onPassiveMiss(String log, Entities.Entity owner, Entities.Entity target, String name) {}
                @Override public void onFighterEnter(boolean isPlayer, Entities.Entity fighter, int remaining) {}
            });

            window.showPanel(new BattlePanel(battle));
            System.out.println("Loaded: " + team.stream()
                    .map(Entities.Entity::getName)
                    .reduce((a,b) -> a + ", " + b).orElse("?")
                    + " vs " + enemy.getName());
        });
    }
}