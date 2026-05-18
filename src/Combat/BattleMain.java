package Combat;

import Combat.NormalBattle.Battle;
import Combat.NormalBattle.BattlePanel;
import Entities.Character;
import Entities.Characters.*;
import Entities.Enemies.*;
import GameMain.GamePanel;

import javax.swing.*;

public class BattleMain {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel window = new GamePanel();

            Character testCharacter = new Zed();

            Battle battle  = new Battle(testCharacter, new Phainon());

            window.showPanel(new BattlePanel(battle));
        });
    }
}