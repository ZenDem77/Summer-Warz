package Combat.CombatTesters;

import Combat.NormalBattle.Battle;
import Combat.NormalBattle.BattlePanel;
import Entities.Artifacts.Artifact;
import Entities.Bosses.*;
import Entities.Character;
import Entities.Characters.*;
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
            Kindle kindle = new Kindle();
            Xyniz xyniz = new Xyniz();
            Zayir zayir = new Zayir();
            Zenzenkoi zenzenkoi = new Zenzenkoi();
            List<Character> team = List.of(kindle, zenzenkoi, zayir, xyniz);

            Weapon weaponZe = new IronEdge();
            Weapon weaponZ = new IronEdge();
            Weapon weaponK = new IronEdge();
            Weapon weapon  = new WolvesGravestone();

            weaponZe.equip(zenzenkoi); weaponZe.setWeaponLevel(20);
            weaponZ.equip(kindle); weaponZ.setWeaponLevel(20);
            weaponK.equip(xyniz); weaponK.setWeaponLevel(20);
            weapon.equip(zayir); weapon.setWeaponLevel(20);

            Artifact a1 = Artifact.generateRandom();
            Artifact a2 = Artifact.generateRandom();
            Artifact a3 = Artifact.generateRandom();
            Artifact a4 = Artifact.generateRandom();

            Artifact b1 = Artifact.generateRandom();
            Artifact b2 = Artifact.generateRandom();
            Artifact b3 = Artifact.generateRandom();
            Artifact b4 = Artifact.generateRandom();

            zenzenkoi.equipArtifact(0, a1);
            zenzenkoi.equipArtifact(1, a2);
            zenzenkoi.equipArtifact(2, a3);
            zenzenkoi.equipArtifact(3, a4);

            kindle.equipArtifact(0, b1);
            kindle.equipArtifact(1, b2);
            kindle.equipArtifact(2, b3);
            kindle.equipArtifact(3, b4);

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