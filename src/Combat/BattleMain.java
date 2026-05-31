package Combat;

import Combat.CharacterBattle.*;
import Combat.NormalBattle.*;
import Entities.Character;
import Entities.Characters.*;
import Entities.Enemies.*;
import Entities.Enemy;
import GameMain.GamePanel;

import javax.swing.*;
import java.util.List;

public class BattleMain {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel window = new GamePanel();

            // Character vs Enemy test battle
            List<Character> team = List.of(new Zed(), new Kaizen());
            List<Enemy> enemies  = List.of(new Phainon(), new Hanzo());
            window.showPanel(new BattlePanel(new Battle(team, enemies)));

            // Character vs Character test battle
            //CharacterBattle battle = new CharacterBattle(new Kaizen(), new Zed()); window.showPanel(new CharacterBattlePanel(battle));
        });
    }
}