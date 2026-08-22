package GameMain;

import Economy.CharacterLevelCost;
import Economy.Currency;
import Economy.LevelingService;
import Economy.Wallet;
import Entities.Character;
import Entities.Sprites.SpriteLoader;
import Entities.PassiveHandler.Passive;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * CharacterLevelUpScreen — modal dialog for leveling up a character.
 * Visual design inspired by classic golden-fantasy RPG UI.
 *
 * Size:   980 × 640, centred.
 * Layout: left 30% = character sprite box, right 70% = stats boxes + button.
 */
public class CharacterLevelUpScreen extends JDialog {

    // ── Theme palette (golden fantasy) ────────────────────────────────────────
    private static final Color BG_MAIN       = new Color(185, 155, 80);
    private static final Color BG_BOX        = new Color(210, 185, 115);
    private static final Color BG_BOX_INNER  = new Color(228, 205, 140);
    private static final Color BORDER_DARK   = new Color(140, 108, 40);
    private static final Color BORDER_LIGHT  = new Color(230, 205, 130);
    private static final Color TEXT_DARK     = new Color(45, 28, 5);
    private static final Color TEXT_MID      = new Color(90, 60, 15);
    private static final Color TEXT_LABEL    = new Color(70, 48, 10);

    // Stat value colors (matching reference image)
    private static final Color COL_HP        = new Color(215, 50,  50);
    private static final Color COL_ATK       = new Color(225, 140, 30);
    private static final Color COL_DEF       = new Color(40,  175, 220);
    private static final Color COL_SPD       = new Color(60,  210, 80);
    private static final Color COL_CRIT      = new Color(220, 205, 40);
    private static final Color COL_CRITDMG   = new Color(210, 155, 35);
    private static final Color COL_BONUS     = new Color(100, 225, 110);
    private static final Color COL_ACCURACY  = new Color(195, 225, 165);
    private static final Color COL_PASSIVE_ON  = new Color(40,  175, 220);
    private static final Color COL_PASSIVE_OFF = new Color(130, 100, 45);

    // Button colors
    private static final Color BTN_GOLD      = new Color(222, 178, 48);
    private static final Color BTN_GOLD_HOV  = new Color(240, 200, 70);
    private static final Color BTN_GOLD_DARK = new Color(160, 120, 25);
    private static final Color BTN_DISABLED  = new Color(130, 118, 95);
    private static final Color BTN_DIS_DARK  = new Color(90, 80, 60);

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int    W           = 980;
    private static final int    H           = 640;
    private static final double LEFT_RATIO  = 0.30;
    private static final int    PAD         = 14;
    private static final int    ROW_H       = 30;
    private static final int    BOX_ARC     = 16;

    // ── Fields ────────────────────────────────────────────────────────────────
    private final Character character;
    private final Wallet    wallet;
    private       JPanel    statsPanel;
    private       JPanel    bottomBarPanel;
    private       JButton   levelUpBtn;
    private       BufferedImage spriteImg;

    // ── Constructor ───────────────────────────────────────────────────────────

    public CharacterLevelUpScreen(Frame parent, Character character, Wallet wallet) {
        super(parent, "Level Up — " + character.getName(), true);
        this.character = character;
        this.wallet    = wallet;

        setSize(W, H);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);  // custom title bar

        // Load sprite
        String idlePath = character.getSpriteSet().idlePath();
        if (idlePath != null && !idlePath.isBlank()) {
            int bw = (int)(W * LEFT_RATIO) - PAD * 4;
            spriteImg = SpriteLoader.load(idlePath, bw, bw);
        }

        buildUI();
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BG_MAIN);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setPreferredSize(new Dimension(W, H));
        setContentPane(root);

        // ── Custom title bar ──────────────────────────────────────────────────
        JPanel titleBar = buildTitleBar();
        titleBar.setBounds(0, 0, W, 52);
        root.add(titleBar);

        int leftW  = (int)(W * LEFT_RATIO);
        int rightW = W - leftW;
        int contentTop = 60;
        int contentH   = H - contentTop - 80;

        // ── Left: sprite box ──────────────────────────────────────────────────
        JPanel spriteBox = buildSpriteBox();
        spriteBox.setBounds(PAD, contentTop, leftW - PAD * 2, contentH);
        root.add(spriteBox);

        // ── Right: stats ──────────────────────────────────────────────────────
        statsPanel = buildStatsPanel(leftW, contentTop, rightW, contentH);
        statsPanel.setBounds(leftW, contentTop, rightW - PAD, contentH);
        root.add(statsPanel);

        // ── Bottom bar ────────────────────────────────────────────────────────
        bottomBarPanel = buildBottomBar();
        bottomBarPanel.setBounds(0, H - 76, W, 76);
        root.add(bottomBarPanel);
    }

    // ── Title bar ─────────────────────────────────────────────────────────────

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(155, 120, 40));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER_DARK);
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
            }
        };

        JLabel title = new JLabel("  ✦  " + character.getName().toUpperCase()
                + "   ·   Lv " + character.getLevel() + "  ✦", JLabel.LEFT);
        title.setFont(new Font("Georgia", Font.BOLD, 20));
        title.setForeground(new Color(255, 235, 150));
        bar.add(title, BorderLayout.CENTER);

        JButton close = new JButton("✕") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(getModel().isRollover()
                        ? new Color(180, 50, 50) : new Color(140, 100, 30));
                g2.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        close.setForeground(new Color(255, 225, 130));
        close.setFont(new Font("SansSerif", Font.BOLD, 16));
        close.setContentAreaFilled(false);
        close.setBorderPainted(false);
        close.setFocusPainted(false);
        close.setPreferredSize(new Dimension(52, 52));
        close.addActionListener(e -> dispose());
        bar.add(close, BorderLayout.EAST);

        return bar;
    }

    // ── Left: sprite box ──────────────────────────────────────────────────────

    private JPanel buildSpriteBox() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Outer box
                g2.setColor(BG_BOX);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), BOX_ARC, BOX_ARC);
                g2.setColor(BORDER_DARK);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, BOX_ARC, BOX_ARC);

                // Inner sprite area
                int ipad = PAD;
                int ix = ipad, iy = ipad + 32;
                int iw = getWidth() - ipad*2, ih = getHeight() - ipad*2 - 80;
                g2.setColor(BG_BOX_INNER);
                g2.fillRoundRect(ix, iy, iw, ih, 10, 10);
                g2.setColor(BORDER_DARK);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(ix, iy, iw, ih, 10, 10);

                // Level label above sprite box
                g2.setFont(new Font("Georgia", Font.BOLD, 17));
                g2.setColor(TEXT_DARK);
                String lvlStr = "Lv.  " + character.getLevel();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(lvlStr, (getWidth() - fm.stringWidth(lvlStr)) / 2, iy - 8);

                // Sprite
                if (spriteImg != null) {
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    int sw = spriteImg.getWidth(), sh = spriteImg.getHeight();
                    double scale = Math.min((double)(iw - 16) / sw, (double)(ih - 16) / sh);
                    int dw = (int)(sw * scale), dh = (int)(sh * scale);
                    int dx = ix + (iw - dw)/2, dy = iy + (ih - dh)/2;
                    g2.drawImage(spriteImg, dx, dy, dw, dh, null);
                } else {
                    g2.setFont(new Font("Georgia", Font.BOLD, 36));
                    g2.setColor(BORDER_DARK);
                    String ini = character.getName().substring(0, 1);
                    FontMetrics fm2 = g2.getFontMetrics();
                    g2.drawString(ini, ix + (iw - fm2.stringWidth(ini))/2, iy + ih/2 + 10);
                }

                // Elixir balance at the bottom of sprite box
                int balance = wallet.getBalance(Currency.ELIXIR);
                g2.setFont(new Font("Georgia", Font.BOLD, 15));
                g2.setColor(COL_DEF);
                String elStr = "Elixir:  " + balance;
                FontMetrics fm3 = g2.getFontMetrics();
                g2.drawString(elStr, (getWidth() - fm3.stringWidth(elStr))/2,
                        getHeight() - 30);
            }
        };
    }

    // ── Right: stats panel ────────────────────────────────────────────────────

    private JPanel buildStatsPanel(int leftW, int contentTop, int rightW, int contentH) {
        JPanel outer = new JPanel(null);
        outer.setOpaque(false);

        int bw = rightW - PAD * 2;
        int cy = PAD / 2;

        // ── Basic Stats box ───────────────────────────────────────────────────
        String[][] basicRows = {
                { "HP",        character.getCurrentHp() + " / " + character.getMaxHp(), colorHex(COL_HP) },
                { "Attack",    String.valueOf(character.getTotalAtk()),                  colorHex(COL_ATK) },
                { "Defense",   String.valueOf(character.getDefense()),                   colorHex(COL_DEF) },
                { "Atk Speed", (character.getAttackSpeed() / 1000.0) + "s",             colorHex(COL_SPD) },
        };
        int basicH = 36 + basicRows.length * ROW_H + PAD;
        JPanel basicBox = buildStatsBox("BASIC STATS", basicRows, bw, basicH);
        basicBox.setBounds(PAD, cy, bw, basicH);
        outer.add(basicBox);
        cy += basicH + PAD / 2;

        // ── Advanced Stats box ────────────────────────────────────────────────
        String[][] advRows = {
                { "Crit Rate",   pct(character.getCritRate()),          colorHex(COL_CRIT)     },
                { "Crit Damage", "+" + pct(character.getCritDamage()),  colorHex(COL_CRITDMG)  },
                { "Dmg Bonus",   "+" + pct(character.getDamageBonus()), colorHex(COL_BONUS)    },
                { "Accuracy",    pct(character.getAccuracy()),          colorHex(COL_ACCURACY) },
        };
        int advH = 36 + advRows.length * ROW_H + PAD;
        JPanel advBox = buildStatsBox("ADVANCED STATS", advRows, bw, advH);
        advBox.setBounds(PAD, cy, bw, advH);
        outer.add(advBox);
        cy += advH + PAD / 2;

        // ── Passives box ──────────────────────────────────────────────────────
        Passive[] passives = character.getPassives();
        if (passives != null && passives.length > 0) {
            int[] unlockLevels = {
                    Character.PASSIVE_1_LEVEL,
                    Character.PASSIVE_2_LEVEL,
                    Character.PASSIVE_3_LEVEL
            };
            int passH = 36 + passives.length * ROW_H + PAD;
            JPanel passBox = buildPassivesBox(passives, unlockLevels, bw, passH);
            passBox.setBounds(PAD, cy, bw, passH);
            outer.add(passBox);
        }

        return outer;
    }

    // ── Stats box ─────────────────────────────────────────────────────────────

    private JPanel buildStatsBox(String sectionTitle, String[][] rows, int bw, int bh) {
        JPanel box = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_BOX);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), BOX_ARC, BOX_ARC);
                g2.setColor(BORDER_DARK);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, BOX_ARC, BOX_ARC);

                // Section title
                g2.setFont(new Font("Georgia", Font.BOLD, 13));
                g2.setColor(TEXT_DARK);
                g2.drawString(sectionTitle, PAD, 22);

                // Divider
                g2.setColor(BORDER_DARK);
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(PAD, 28, getWidth() - PAD, 28);

                // Rows
                int ry = 28 + ROW_H / 2 + 6;
                for (String[] row : rows) {
                    g2.setFont(new Font("Georgia", Font.BOLD, 15));
                    g2.setColor(TEXT_LABEL);
                    g2.drawString(row[0], PAD + 4, ry);

                    g2.setFont(new Font("Georgia", Font.BOLD, 15));
                    Color vc = Color.decode(row[2]);
                    g2.setColor(vc);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(row[1], getWidth() - PAD - fm.stringWidth(row[1]), ry);

                    // Subtle row divider
                    g2.setColor(new Color(160, 128, 55, 80));
                    g2.drawLine(PAD, ry + 6, getWidth() - PAD, ry + 6);
                    ry += ROW_H;
                }
            }
        };
        box.setPreferredSize(new Dimension(bw, bh));
        return box;
    }

    // ── Passives box ─────────────────────────────────────────────────────────

    private JPanel buildPassivesBox(Passive[] passives, int[] unlockLevels, int bw, int bh) {
        JPanel box = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_BOX);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), BOX_ARC, BOX_ARC);
                g2.setColor(BORDER_DARK);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, BOX_ARC, BOX_ARC);

                g2.setFont(new Font("Georgia", Font.BOLD, 13));
                g2.setColor(TEXT_DARK);
                g2.drawString("PASSIVES", PAD, 22);
                g2.setColor(BORDER_DARK);
                g2.setStroke(new BasicStroke(1f));
                g2.drawLine(PAD, 28, getWidth() - PAD, 28);

                int ry = 28 + ROW_H / 2 + 6;
                for (int i = 0; i < passives.length; i++) {
                    if (passives[i] == null) continue;
                    boolean unlocked = character.getLevel() >= unlockLevels[i];

                    g2.setFont(new Font("Georgia", Font.BOLD, 15));
                    g2.setColor(unlocked ? COL_PASSIVE_ON : COL_PASSIVE_OFF);
                    g2.drawString(passives[i].getName(), PAD + 4, ry);

                    String tag = unlocked ? "UNLOCKED" : "Lv" + unlockLevels[i];
                    g2.setFont(new Font("Georgia", Font.BOLD, 12));
                    g2.setColor(unlocked ? COL_BONUS : new Color(160, 90, 30));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(tag, getWidth() - PAD - fm.stringWidth(tag), ry);

                    g2.setColor(new Color(160, 128, 55, 80));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawLine(PAD, ry + 6, getWidth() - PAD, ry + 6);
                    ry += ROW_H;
                }
            }
        };
        box.setPreferredSize(new Dimension(bw, bh));
        return box;
    }

    // ── Bottom bar ────────────────────────────────────────────────────────────

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(null) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(new Color(155, 120, 40));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER_DARK);
                g2.drawLine(0, 0, getWidth(), 0);
            }
        };

        boolean maxLevel = character.getLevel() >= Character.MAX_LEVEL;
        int     cost     = maxLevel ? 0 : CharacterLevelCost.getCost(character.getLevel());
        boolean canAfford = !maxLevel && wallet.hasEnough(Currency.ELIXIR, cost);

        String btnText = maxLevel ? "Max Level Reached"
                : "Level Up  (Cost: " + cost + " Elixir)";

        levelUpBtn = goldenButton(btnText, canAfford && !maxLevel);
        levelUpBtn.setBounds(W - 280 - PAD, 14, 280, 48);
        levelUpBtn.setEnabled(!maxLevel);
        levelUpBtn.addActionListener(e -> performLevelUp());
        bar.add(levelUpBtn);

        return bar;
    }

    // ── Golden button ─────────────────────────────────────────────────────────

    private JButton goldenButton(String text, boolean active) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = !isEnabled()      ? BTN_DISABLED
                        : getModel().isPressed() ? BTN_GOLD.darker()
                        : getModel().isRollover() ? BTN_GOLD_HOV
                        : active ? BTN_GOLD : BTN_DISABLED;
                Color border = !isEnabled() || !active ? BTN_DIS_DARK : BTN_GOLD_DARK;
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(border);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 12, 12);
                // Highlight line at top
                if (active && isEnabled()) {
                    g2.setColor(new Color(255, 245, 180, 120));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawLine(8, 3, getWidth()-8, 3);
                }
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Georgia", Font.BOLD, 16));
        btn.setForeground(active ? TEXT_DARK : new Color(160, 145, 110));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(active ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
        return btn;
    }

    // ── Level-up action ───────────────────────────────────────────────────────

    private void performLevelUp() {
        int cost = CharacterLevelCost.getCost(character.getLevel());
        if (!wallet.hasEnough(Currency.ELIXIR, cost)) { showToast("Not enough Elixir!"); return; }
        LevelingService.LevelUpResult result = LevelingService.levelUpCharacter(character, wallet);
        if (result == LevelingService.LevelUpResult.MAX_LEVEL_REACHED) {
            showToast("Already at max level!"); return;
        } else if (result != LevelingService.LevelUpResult.SUCCESS) {
            showToast("Level up failed!"); return;
        }
        refreshUI();
    }

    private void refreshUI() {
        JPanel root = (JPanel) getContentPane();

        // Update title
        JPanel titleBar = (JPanel) root.getComponent(0);
        if (titleBar.getComponent(0) instanceof JLabel lbl)
            lbl.setText("  ✦  " + character.getName().toUpperCase()
                    + "   ·   Lv " + character.getLevel() + "  ✦");

        // Remove old stats panel and bottom bar by reference (not by index)
        // so shifting component indices can never cause the wrong thing to be removed.
        root.remove(statsPanel);
        root.remove(bottomBarPanel);

        // Rebuild stats
        int leftW  = (int)(W * LEFT_RATIO);
        int rightW = W - leftW;
        statsPanel = buildStatsPanel(leftW, 60, rightW, H - 60 - 80);
        statsPanel.setBounds(leftW, 60, rightW - PAD, H - 60 - 80);
        root.add(statsPanel);

        // Rebuild bottom bar (button label + cost update after level-up)
        bottomBarPanel = buildBottomBar();
        bottomBarPanel.setBounds(0, H - 76, W, 76);
        root.add(bottomBarPanel);

        root.revalidate();
        root.repaint();
    }

    // ── Toast ─────────────────────────────────────────────────────────────────

    private void showToast(String message) {
        JWindow toast = new JWindow(this);
        JLabel label  = new JLabel("  " + message + "  ", JLabel.CENTER);
        label.setFont(new Font("Georgia", Font.BOLD, 14));
        label.setForeground(new Color(255, 235, 150));
        label.setOpaque(true);
        label.setBackground(new Color(140, 50, 30));
        label.setBorder(BorderFactory.createLineBorder(BORDER_DARK, 2));
        toast.add(label);
        toast.pack();
        Point loc = levelUpBtn.getLocationOnScreen();
        toast.setLocation(loc.x, loc.y - 44);
        toast.setVisible(true);
        Timer hide = new Timer(2000, e -> toast.dispose());
        hide.setRepeats(false);
        hide.start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String pct(double value) {
        return Math.round(value * 100) + "%";
    }

    private static String colorHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}