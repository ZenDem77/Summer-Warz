package Combat;

import Combat.CharacterBattle.*;
import Combat.NormalBattle.*;
import Entities.Character;
import Entities.Characters.*;
import Entities.Enemies.*;
import GameMain.GamePanel;

import javax.swing.*;

public class BattleMain {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel window = new GamePanel();

            // Character vs Enemy test battle
            Battle battle  = new Battle(new Zed(), new Phainon()); window.showPanel(new BattlePanel(battle));

            // Character vs Character test battle
            //CharacterBattle battle = new CharacterBattle(new Kaizen(), new Zed()); window.showPanel(new CharacterBattlePanel(battle));
        });
    }
}