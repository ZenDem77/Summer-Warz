package Combat.CombatTesters;

import Combat.NormalBattle.Battle;
import Combat.NormalBattle.BattlePanel;
import Entities.Bosses.Lynx;
import Entities.Character;
import Entities.Characters.Kaizen;
import Entities.Enemy;
import GameMain.GamePanel;

import javax.swing.*;

/**
 * EnemyMainTester — fight any single enemy of your choice (1 character vs 1 enemy).
 *
 * Useful for testing a specific enemy's passive/stats in isolation without
 * going through Tower of Suffering's floor progression.
 *
 * ── To customize ─────────────────────────────────────────────────────────
 * Edit the two lines marked below: pick your character and pick your enemy.
 * Available characters: Zed, Kaizen, Zayir
 * Available bosses:     Phainon, Hanzo, Lynx
 * Available mobs (tier 1-10): UnknownSubject(tier), ExperimentalSubject(tier),
 *                              IndestructibleSubject(tier)
 */
public class EnemyMainTester {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel window = new GamePanel();

            // ═══════════════════════════════════════════════════════════════════
            //  EDIT THESE TWO LINES TO CHOOSE YOUR MATCHUP
            // ═══════════════════════════════════════════════════════════════════
            Character player = new Kaizen();
            Enemy     enemy  = new Lynx();
            // ═══════════════════════════════════════════════════════════════════

            Battle battle = new Battle(player, enemy);

            battle.addListener(new Battle.BattleListener() {
                @Override
                public void onBattleEnd(Battle.BattleState result) {
                    String msg = result == Battle.BattleState.PLAYER_WIN
                            ? "🏆 " + player.getName() + " wins!"
                            : "💀 " + enemy.getName() + " wins!";
                    System.out.println(msg);
                }
                @Override public void onPlayerAttack(String log, int dmg, boolean crit, boolean miss) {}
                @Override public void onEnemyAttack (String log, int dmg, boolean crit, boolean miss) {}
                @Override public void onPassive(String log, Entities.Entity owner, int amt, boolean heal) {}
                @Override public void onPassiveMiss(String log, Entities.Entity owner, Entities.Entity target, String passiveName) {}
                @Override public void onFighterEnter(boolean isPlayer, Entities.Entity fighter, int remaining) {}
            });

            window.showPanel(new BattlePanel(battle));
            System.out.println("Loaded: " + player.getName() + " vs " + enemy.getName());
        });
    }
}