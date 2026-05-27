package GameMain;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GamePanel window = new GamePanel();
            window.showPanel(new TitleScreen());
        });
    }
}