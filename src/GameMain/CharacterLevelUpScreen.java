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
 *
 * Size:     50% screen width × 80% screen height, centred.
 * Layout:   left 30% = character sprite, right 70% = stats + level-up button.
 * Theme:    matches the battle panel dark aesthetic (dark backgrounds,
 *           gold/white text, rounded buttons).
 *
 * Usage:
 *   CharacterLevelUpScreen screen = new CharacterLevelUpScreen(parent, character, wallet);
 *   screen.setVisible(true);  // blocks until dismissed
 */
public class CharacterLevelUpScreen extends JDialog {

    // ── Theme colors (match battle panels) ───────────────────────────────────
    private static final Color BG_DARK      = new Color(18, 18, 28);
    private static final Color BG_PANEL     = new Color(26, 26, 40);
    private static final Color BG_CARD      = new Color(35, 35, 52);
    private static final Color ACCENT_GOLD  = new Color(200, 170, 80);
    private static final Color ACCENT_BLUE  = new Color(80, 150, 255);
    private static final Color TEXT_PRIMARY = new Color(230, 230, 230);
    private static final Color TEXT_DIM     = new Color(140, 140, 155);
    private static final Color TEXT_GREEN   = new Color(80, 210, 80);
    private static final Color TEXT_RED     = new Color(220, 70, 70);
    private static final Color DIVIDER      = new Color(55, 55, 75);
    private static final Color BTN_NORMAL   = new Color(60, 90, 50);
    private static final Color BTN_HOVER    = new Color(80, 120, 65);
    private static final Color BTN_DISABLED = new Color(45, 45, 55);

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final double LEFT_RATIO  = 0.30;
    private static final int    PADDING     = 24;
    private static final int    ROW_H       = 36;

    // ── Fields ────────────────────────────────────────────────────────────────
    private final Character       character;
    private final Wallet          wallet;

    // UI refs that need refreshing after level-up
    private       JPanel      statsPanel;
    private       JButton     levelUpBtn;
    private       JLabel      spriteLabel;
    private       BufferedImage spriteImg;

    // ── Constructor ───────────────────────────────────────────────────────────

    public CharacterLevelUpScreen(Frame parent, Character character, Wallet wallet) {
        super(parent, "Level Up — " + character.getName(), true);  // modal = true
        this.character      = character;
        this.wallet         = wallet;

        // ── Window sizing & position ──────────────────────────────────────────
        int w = 576;
        int h = 640;
        setSize(w, h);
        setLocationRelativeTo(null);   // centre on screen
        setResizable(false);
        setUndecorated(false);
        getContentPane().setBackground(BG_DARK);

        // ── Load sprite ───────────────────────────────────────────────────────
        String idlePath = character.getSpriteSet().idlePath();
        if (idlePath != null && !idlePath.isBlank()) {
            int spriteBoxW = (int)(w * LEFT_RATIO) - PADDING * 2;
            int spriteBoxH = spriteBoxW;
            spriteImg = SpriteLoader.load(idlePath, spriteBoxW, spriteBoxH);
        }

        buildUI(w, h);
    }

    // ── UI construction ───────────────────────────────────────────────────────

    private void buildUI(int w, int h) {
        setLayout(new BorderLayout());

        // ── Title bar ─────────────────────────────────────────────────────────
        JPanel titleBar = buildTitleBar();
        add(titleBar, BorderLayout.NORTH);

        // ── Main content: left sprite + right stats ───────────────────────────
        JPanel content = new JPanel(null);
        content.setBackground(BG_DARK);
        add(content, BorderLayout.CENTER);

        // Left panel — sprite area
        JPanel leftPanel = buildLeftPanel();
        int leftW = (int)(w * LEFT_RATIO);
        int mainH = h - 60 - 80;  // minus title + button bar
        leftPanel.setBounds(0, 0, leftW, mainH);
        content.add(leftPanel);

        // Right panel — stats
        statsPanel = buildStatsPanel();
        statsPanel.setBounds(leftW, 0, w - leftW, mainH);
        content.add(statsPanel);

        // Vertical divider
        JPanel divider = new JPanel();
        divider.setBackground(DIVIDER);
        divider.setBounds(leftW - 1, PADDING, 1, mainH - PADDING * 2);
        content.add(divider);

        // ── Bottom bar — level-up button ───────────────────────────────────────
        JPanel bottomBar = buildBottomBar();
        add(bottomBar, BorderLayout.SOUTH);
    }

    private JPanel buildTitleBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_PANEL);
        bar.setPreferredSize(new Dimension(0, 56));
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, DIVIDER));

        JLabel title = new JLabel(character.getName() + "  ·  Lv " + character.getLevel(), JLabel.LEFT);
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setForeground(ACCENT_GOLD);
        title.setBorder(BorderFactory.createEmptyBorder(0, PADDING, 0, 0));
        bar.add(title, BorderLayout.CENTER);

        // Close button
        JButton close = styledButton("✕", false);
        close.setPreferredSize(new Dimension(56, 56));
        close.setFont(new Font("SansSerif", Font.PLAIN, 22));
        close.addActionListener(e -> dispose());
        close.setBackground(BG_PANEL);
        close.setBorderPainted(false);
        bar.add(close, BorderLayout.EAST);

        return bar;
    }

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

                if (spriteImg != null) {
                    int iw = spriteImg.getWidth();
                    int ih = spriteImg.getHeight();
                    // Fit the sprite within the panel, maintaining aspect ratio
                    double scale = Math.min((double)(getWidth() - PADDING * 2) / iw,
                            (double)(getHeight() - PADDING * 2) / ih);
                    int dw = (int)(iw * scale), dh = (int)(ih * scale);
                    int dx = (getWidth()  - dw) / 2;
                    int dy = (getHeight() - dh) / 2;
                    g2.drawImage(spriteImg, dx, dy, dw, dh, null);
                } else {
                    // Placeholder if no sprite loaded
                    g2.setColor(BG_CARD);
                    int pad = PADDING * 2;
                    g2.fillRoundRect(pad, pad, getWidth() - pad * 2, getHeight() - pad * 2, 16, 16);
                    g2.setColor(TEXT_DIM);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 32));
                    String initial = character.getName().substring(0, 1);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(initial,
                            (getWidth()  - fm.stringWidth(initial)) / 2,
                            (getHeight() + fm.getAscent()) / 2 - 4);
                }
            }
        };
        panel.setBackground(BG_DARK);
        return panel;
    }

    private JPanel buildStatsPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_DARK);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        // ── Section: Core stats ───────────────────────────────────────────────
        panel.add(sectionLabel("BASIC STATS"));
        panel.add(Box.createVerticalStrut(8));

        panel.add(statRow("HP",           character.getCurrentHp() + " / " + character.getMaxHp(), TEXT_GREEN));
        panel.add(statRow("Attack",       String.valueOf(character.getTotalAtk()),       TEXT_PRIMARY));
        panel.add(statRow("Defense",      String.valueOf(character.getDefense()),        TEXT_PRIMARY));
        panel.add(statRow("Atk Speed",    (character.getAttackSpeed() / 1000.0) + "s",  TEXT_PRIMARY));

        panel.add(Box.createVerticalStrut(6));
        panel.add(dividerLine());
        panel.add(Box.createVerticalStrut(6));

        // ── Section: Crit & Accuracy ──────────────────────────────────────────
        panel.add(sectionLabel("ADVANCED STATS"));
        panel.add(Box.createVerticalStrut(8));

        panel.add(statRow("Crit Rate",    pct(character.getCritRate()),                  TEXT_PRIMARY));
        panel.add(statRow("Crit Damage",  "+" + pct(character.getCritDamage()),          TEXT_PRIMARY));
        panel.add(statRow("Dmg Bonus",    "+" + pct(character.getDamageBonus()),         TEXT_PRIMARY));
        panel.add(statRow("Accuracy",     pct(character.getAccuracy()),                  TEXT_PRIMARY));

        // ── Section: Passives ─────────────────────────────────────────────────
        Passive[] passives = character.getPassives();
        if (passives != null && passives.length > 0) {
            panel.add(Box.createVerticalStrut(6));
            panel.add(dividerLine());
            panel.add(Box.createVerticalStrut(6));
            panel.add(sectionLabel("PASSIVES"));
            panel.add(Box.createVerticalStrut(8));

            int[] unlockLevels = {
                    Character.PASSIVE_1_LEVEL,
                    Character.PASSIVE_2_LEVEL,
                    Character.PASSIVE_3_LEVEL
            };
            for (int i = 0; i < passives.length; i++) {
                if (passives[i] == null) continue;
                boolean unlocked = character.getLevel() >= unlockLevels[i];
                panel.add(passiveRow(passives[i].getName(), unlockLevels[i], unlocked));
            }
        }

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildBottomBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_PANEL);
        bar.setPreferredSize(new Dimension(0, 72));
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DIVIDER));

        // Elixir balance
        int balance = wallet.getBalance(Currency.ELIXIR);
        JLabel balanceLabel = new JLabel("  Elixir: " + balance, JLabel.LEFT);
        balanceLabel.setFont(new Font("SansSerif", Font.PLAIN, 17));
        balanceLabel.setForeground(ACCENT_BLUE);
        bar.add(balanceLabel, BorderLayout.WEST);

        // Level-up button
        levelUpBtn = buildLevelUpButton();
        JPanel btnWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, PADDING, 12));
        btnWrapper.setBackground(BG_PANEL);
        btnWrapper.add(levelUpBtn);
        bar.add(btnWrapper, BorderLayout.EAST);

        return bar;
    }

    private JButton buildLevelUpButton() {
        int     cost      = CharacterLevelCost.getCost(character.getLevel());
        boolean canAfford = wallet.hasEnough(Currency.ELIXIR, cost);

        String label = "Level Up  (Cost: " + cost + " Elixir)";

        JButton btn = styledButton(label, canAfford);
        btn.setPreferredSize(new Dimension(260, 46));

        btn.addActionListener(e -> performLevelUp());
        return btn;
    }

    // ── Level-up action ───────────────────────────────────────────────────────

    private void performLevelUp() {
        int cost = CharacterLevelCost.getCost(character.getLevel());

        if (!wallet.hasEnough(Currency.ELIXIR, cost)) {
            showToast("Not enough Elixir!");
            return;
        }

        LevelingService.LevelUpResult result = LevelingService.levelUpCharacter(character, wallet);
        if (result == LevelingService.LevelUpResult.MAX_LEVEL_REACHED) {
            showToast("Already at max level!");
            return;
        } else if (result != LevelingService.LevelUpResult.SUCCESS) {
            showToast("Level up failed!");
            return;
        }

        // Refresh UI
        refreshUI();
    }

    private void refreshUI() {
        // Rebuild stats panel in place
        Container content = getContentPane().getComponent(1) instanceof JPanel p ? p : null;
        if (content == null) { dispose(); return; }

        int leftW = (int)(getWidth() * LEFT_RATIO);
        content.remove(statsPanel);
        statsPanel = buildStatsPanel();
        statsPanel.setBounds(leftW, 0, getWidth() - leftW, getHeight() - 60 - 80);
        content.add(statsPanel);

        // Refresh title
        JPanel titleBar = (JPanel) getContentPane().getComponent(0);
        ((JLabel) titleBar.getComponent(0)).setText(
                character.getName() + "  ·  Lv " + character.getLevel());

        // Rebuild bottom bar entirely — Elixir label and button are both
        // inside btnWrapper, so the simplest correct refresh is a full rebuild
        remove(getContentPane().getComponent(2));
        add(buildBottomBar(), BorderLayout.SOUTH);

        revalidate();
        repaint();
    }

    // ── Toast notification ────────────────────────────────────────────────────

    private void showToast(String message) {
        JWindow toast = new JWindow(this);
        JLabel label  = new JLabel("  " + message + "  ", JLabel.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 17));
        label.setForeground(Color.WHITE);
        label.setOpaque(true);
        label.setBackground(new Color(180, 60, 60));
        label.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        toast.add(label);
        toast.pack();

        // Position above the level-up button
        Point loc = levelUpBtn.getLocationOnScreen();
        toast.setLocation(loc.x, loc.y - 50);
        toast.setVisible(true);

        Timer hide = new Timer(2000, e -> toast.dispose());
        hide.setRepeats(false);
        hide.start();
    }

    // ── Helper builders ───────────────────────────────────────────────────────

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        lbl.setForeground(TEXT_DIM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel statRow(String label, String value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG_DARK);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 17));
        lbl.setForeground(TEXT_DIM);

        JLabel val = new JLabel(value, JLabel.RIGHT);
        val.setFont(new Font("SansSerif", Font.BOLD, 17));
        val.setForeground(valueColor);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private JPanel passiveRow(String name, int unlockLevel, boolean unlocked) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG_DARK);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(name);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 16));
        lbl.setForeground(unlocked ? ACCENT_BLUE : TEXT_DIM);

        JLabel lock = new JLabel(unlocked ? "UNLOCKED" : "Lv" + unlockLevel, JLabel.RIGHT);
        lock.setFont(new Font("SansSerif", Font.BOLD, 14));
        lock.setForeground(unlocked ? TEXT_GREEN : TEXT_RED);

        row.add(lbl,  BorderLayout.WEST);
        row.add(lock, BorderLayout.EAST);
        return row;
    }

    private JPanel dividerLine() {
        JPanel line = new JPanel();
        line.setBackground(DIVIDER);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }

    private JButton styledButton(String text, boolean active) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = !isEnabled() ? BTN_DISABLED
                        : getModel().isPressed() ? BTN_NORMAL.darker()
                        : getModel().isRollover() ? BTN_HOVER
                        : (active ? BTN_NORMAL : BTN_DISABLED);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(active && isEnabled() ? ACCENT_GOLD : TEXT_DIM);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 17));
        btn.setForeground(active ? Color.WHITE : TEXT_DIM);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(active ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                : Cursor.getDefaultCursor());
        return btn;
    }

    private static String pct(double value) {
        int rounded = (int) Math.round(value * 100);
        return rounded + "%";
    }
}