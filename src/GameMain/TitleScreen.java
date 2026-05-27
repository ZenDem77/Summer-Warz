package GameMain;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/**
 * TitleScreen — the main menu screen for Ninja Warz.
 *
 * Displays:
 *  - A placeholder background (swap with /res/title_bg.gif or .png later)
 *  - A placeholder title logo (swap with /res/title_logo.png later)
 *  - Three centred menu buttons: Start Game, Settings, Quit Game
 *
 * Navigation is handled via GamePanel.get().showPanel(...).
 */
public class TitleScreen extends JPanel {

    private static final int W = GamePanel.WIDTH;
    private static final int H = GamePanel.HEIGHT;

    // ── Placeholder assets ────────────────────────────────────────────────────
    private final BufferedImage bgImage;
    private final BufferedImage logoImage;

    // ── Button styling ────────────────────────────────────────────────────────
    private static final int   BTN_W       = 280;
    private static final int   BTN_H       = 54;
    private static final int   BTN_GAP     = 20;
    private static final int   BTN_ARC     = 12;
    private static final Font  BTN_FONT    = new Font("SansSerif", Font.BOLD, 20);
    private static final Color BTN_NORMAL  = new Color(30, 30, 30, 210);
    private static final Color BTN_HOVER   = new Color(60, 60, 60, 230);
    private static final Color BTN_PRESSED = new Color(15, 15, 15, 240);
    private static final Color BTN_BORDER  = new Color(180, 160, 100);
    private static final Color BTN_TEXT    = new Color(230, 210, 150);

    // ── Button state ──────────────────────────────────────────────────────────
    private final String[]  labels  = { "Start Game", "Settings", "Quit Game" };
    private final boolean[] hovered = new boolean[labels.length];
    private final boolean[] pressed = new boolean[labels.length];

    // ─────────────────────────────────────────────────────────────────────────

    public TitleScreen() {
        setPreferredSize(new Dimension(W, H));
        setLayout(null);

        bgImage   = makePlaceholderBg();
        logoImage = makePlaceholderLogo();

        wireInput();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    private void wireInput() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean changed = false;
                for (int i = 0; i < labels.length; i++) {
                    boolean hit = btnBounds(i).contains(e.getPoint());
                    if (hovered[i] != hit) { hovered[i] = hit; changed = true; }
                }
                if (changed) repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                for (int i = 0; i < labels.length; i++) {
                    if (btnBounds(i).contains(e.getPoint())) {
                        pressed[i] = true; repaint();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                for (int i = 0; i < labels.length; i++) {
                    if (pressed[i] && btnBounds(i).contains(e.getPoint())) onButtonClick(i);
                    pressed[i] = false;
                }
                repaint();
            }
        });
    }

    private void onButtonClick(int index) {
        switch (index) {
            case 0 -> onStartGame();
            case 1 -> onSettings();
            case 2 -> onQuit();
        }
    }

    private void onStartGame() {
        // TODO: navigate to character select or directly to battle
        // Example: GamePanel.get().showPanel(new BattlePanel(...));
        System.out.println("Start Game — wire up navigation here.");
    }

    private void onSettings() {
        // TODO: show settings screen
        System.out.println("Settings — wire up settings screen here.");
    }

    private void onQuit() {
        System.exit(0);
    }

    // ── Button bounds ─────────────────────────────────────────────────────────

    private Rectangle btnBounds(int i) {
        int totalH = labels.length * BTN_H + (labels.length - 1) * BTN_GAP;
        int startY = H / 2 - totalH / 2 + 80;   // slightly below centre, leaving room for logo
        return new Rectangle((W - BTN_W) / 2, startY + i * (BTN_H + BTN_GAP), BTN_W, BTN_H);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  paintComponent
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Background
        g2.drawImage(bgImage, 0, 0, null);

        // 2. Logo
        g2.drawImage(logoImage,
                (W - logoImage.getWidth()) / 2,
                H / 2 - logoImage.getHeight() / 2 - 160,
                null);

        // 3. Buttons
        for (int i = 0; i < labels.length; i++) drawButton(g2, i);
    }

    // ── Draw: button ──────────────────────────────────────────────────────────

    private void drawButton(Graphics2D g2, int i) {
        Rectangle r = btnBounds(i);

        // Fill
        g2.setColor(pressed[i] ? BTN_PRESSED : hovered[i] ? BTN_HOVER : BTN_NORMAL);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, BTN_ARC, BTN_ARC);

        // Border
        g2.setColor(hovered[i] ? BTN_BORDER.brighter() : BTN_BORDER);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, BTN_ARC, BTN_ARC);
        g2.setStroke(new BasicStroke(1));

        // Label
        g2.setFont(BTN_FONT);
        FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + (r.width  - fm.stringWidth(labels[i])) / 2;
        int ty = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(labels[i], tx + 1, ty + 1);
        g2.setColor(hovered[i] ? Color.WHITE : BTN_TEXT);
        g2.drawString(labels[i], tx, ty);
    }

    // ── Placeholder asset generators ──────────────────────────────────────────

    /**
     * Dark gradient background with a soft vignette.
     * Replace with:
     *   bgImage = ImageIO.read(getClass().getResource("/res/title_bg.png"));
     */
    private BufferedImage makePlaceholderBg() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        GradientPaint gp = new GradientPaint(0, 0, new Color(10, 10, 20),
                0, H, new Color(30, 20, 10));
        g.setPaint(gp);
        g.fillRect(0, 0, W, H);

        g.setFont(new Font("SansSerif", Font.ITALIC, 13));
        g.setColor(new Color(255, 255, 255, 40));
        String hint = "[ Title Background Placeholder — replace with /res/title_bg.png ]";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(hint, (W - fm.stringWidth(hint)) / 2, H - 16);

        g.dispose();
        return img;
    }

    /**
     * Text-based title logo placeholder.
     * Replace with:
     *   logoImage = ImageIO.read(getClass().getResource("/res/title_logo.png"));
     */
    private BufferedImage makePlaceholderLogo() {
        int lw = 640, lh = 160;
        BufferedImage img = new BufferedImage(lw, lh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String title = "NINJA WARZ";
        g.setFont(new Font("SansSerif", Font.BOLD, 80));
        FontMetrics fm = g.getFontMetrics();
        int tx = (lw - fm.stringWidth(title)) / 2;
        int ty = lh / 2 + fm.getAscent() / 2 - 10;

        // Drop shadow
        g.setColor(new Color(0, 0, 0, 180));
        g.drawString(title, tx + 4, ty + 4);

        // Gold gradient fill
        GradientPaint gold = new GradientPaint(0, ty - fm.getAscent(), new Color(255, 230, 100),
                0, ty,                  new Color(180, 130, 30));
        g.setPaint(gold);
        g.drawString(title, tx, ty);

        // Placeholder hint
        g.setFont(new Font("SansSerif", Font.ITALIC, 12));
        g.setColor(new Color(255, 255, 255, 60));
        String hint = "[ Title Logo Placeholder — replace with /res/title_logo.png ]";
        fm = g.getFontMetrics();
        g.drawString(hint, (lw - fm.stringWidth(hint)) / 2, lh - 8);

        g.dispose();
        return img;
    }
}
