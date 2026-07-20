package Combat.CombatTesters;

import Combat.CharacterBattle.CharacterBattle;
import Combat.CharacterBattle.CharacterBattlePanel;
import Entities.Character;
import Entities.Characters.*;
import GameMain.GamePanel;
import GameModes.Spar.SparMode;

import javax.swing.*;

public class SparMainTester {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel window = new GamePanel();

            // ── Choose the two fighters ────────────────────────────────────────
            Character fighter1 = new Zayir();
            Character fighter2 = new Zenzenkoi();

            // ── Create SparMode ───────────────────────────────────────────────
            SparMode mode = new SparMode(fighter1, fighter2);

            mode.addListener(new SparMode.SparModeListener() {
                @Override
                public void onSparStart(Character f1, Character f2) {
                    System.out.println("▶ Spar started: " + f1.getName() + " vs " + f2.getName());
                }

                @Override
                public void onSparEnd(Character winner, Character loser) {
                    System.out.println("🏆 " + winner.getName() + " defeated " + loser.getName() + "!");
                }
            });

            // ── Start the spar ────────────────────────────────────────────────
            CharacterBattle battle = mode.startSpar();

            // Forward the battle's result back into SparMode
            battle.addListener(new CharacterBattle.BattleListener() {
                @Override
                public void onBattleEnd(CharacterBattle.BattleState result) {
                    SwingUtilities.invokeLater(() -> mode.onBattleEnd(result));
                }
                @Override public void onFighter1Attack(String log, int dmg, boolean crit, boolean miss) {}
                @Override public void onFighter2Attack(String log, int dmg, boolean crit, boolean miss) {}
                @Override public void onPassive(String log, Entities.Entity owner, int amt, boolean heal) {}
                @Override public void onPassiveMiss(String log, Entities.Entity owner, Entities.Entity target, String passiveName) {}
                @Override public void onSpecialHit(String log, Entities.Entity owner, Entities.Entity target, int amount, boolean isCrit) {}
            });

            window.showPanel(new CharacterBattlePanel(battle));
            System.out.println("Spar loaded: " + fighter1.getName() + " vs " + fighter2.getName());
        });
    }
}