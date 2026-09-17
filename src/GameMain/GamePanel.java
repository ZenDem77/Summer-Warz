package GameMain;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JFrame {

    // ── Window constants ──────────────────────────────────────────────────────
    public static final int WIDTH  = 1280;
    public static final int HEIGHT = 720;
    public static final String TITLE = "Ninja Warz";

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static GamePanel instance;

    public static GamePanel get() { return instance; }

    // ── Root content area ─────────────────────────────────────────────────────
    private final JPanel root;

    // ── Constructor ───────────────────────────────────────────────────────────

    public GamePanel() {
        super(TITLE);
        instance = this;

        // ── Window setup ──────────────────────────────────────────────────────
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // ── Root panel ────────────────────────────────────────────────────────
        root = new JPanel(new BorderLayout());
        root.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        root.setBackground(Color.BLACK);
        setContentPane(root);

        pack();
        setLocationRelativeTo(null);   // centre on screen
        setVisible(true);
    }

    // ── Panel management ──────────────────────────────────────────────────────
    public void showPanel(JPanel panel) {
        root.removeAll();
        root.add(panel, BorderLayout.CENTER);
        root.revalidate();
        root.repaint();
    }
}