package Combat.CombatTesters;

import Combat.NormalBattle.Battle;
import Combat.NormalBattle.BattlePanel;
import Entities.Artifacts.Artifact;
import Entities.Bosses.*;
import Entities.Character;
import Entities.Characters.*;
import Entities.Enemies.IndestructibleSubject;
import Entities.Enemy;
import Entities.Weapons.*;
import Entities.Weapons.Attack.Sword;
import Entities.Weapons.CritRate.Saber;
import Entities.Weapons.Hp.Staff;
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
            Kouzen kouzen = new Kouzen();
            List<Character> team = List.of(kouzen, kindle, zayir, xyniz);

            Weapon staff = new Staff();
            Weapon weaponZe = new Saber();
            Weapon weaponZ = new Saber();
            Weapon weaponK = new Saber();
            Weapon weapon  = new Sword();

            staff.equip(kouzen); staff.setWeaponLevel(20);
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

            xyniz.equipArtifact(0, a1);
            xyniz.equipArtifact(1, a2);
            xyniz.equipArtifact(2, a3);
            xyniz.equipArtifact(3, a4);

            kindle.equipArtifact(0, b1);
            kindle.equipArtifact(1, b2);
            kindle.equipArtifact(2, b3);
            kindle.equipArtifact(3, b4);

            // ═══════════════════════════════════════════════════════════════════
            //  ENEMY
            // ═══════════════════════════════════════════════════════════════════
            Enemy enemy = new Stain();

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
                @Override public void onSpecialHit(String log, Entities.Entity owner, Entities.Entity target, int amount, boolean isCrit) {}
                @Override public void onImmune(String log, Entities.Entity owner, Entities.Entity target, String passiveName) {}
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