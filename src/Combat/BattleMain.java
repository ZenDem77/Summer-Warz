package Combat;

import Entities.*;

import javax.swing.*;

public class BattleMain {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Zed zed    = new Zed();
            Phainon phainon = new Phainon();
            Battle battle  = new Battle(zed, phainon);

            JFrame frame = new JFrame("Summer Warz – Battle");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(new BattlePanel(battle));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}