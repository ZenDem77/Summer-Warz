package Combat.CharacterBattle;

import Combat.CharacterBattle.CharacterBattle;
import Entities.Character;
import Entities.Entity;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * CharacterBattlePanel — GUI for a Character vs Character fight.
 *
 * Functionally identical to BattlePanel but backed by CharacterBattle.
 * Both sides are displayed with their clan tag instead of a rank.
 * Both sides run their full active passive kits.
 */
public class CharacterBattlePanel extends JPanel implements CharacterBattle.BattleListener {

    // ── Timing ────────────────────────────────────────────────────────────────
    private static final int APPROACH_STEP_MS = 16;
    private static final int APPROACH_SPEED   = 4;
    private static final int FIGHTER_SIZE     = 48;
    private static final int ENGAGE_GAP       = 10;
    private static final int HIT_FLASH_MS     = 120;
    private static final int LOG_LIMIT        = 200;

    // ── Colors ────────────────────────────────────────────────────────────────
    // Both fighters are characters — use two distinct blues
    private static final Color F1_COLOR       = new Color(40, 100, 220);
    private static final Color F1_FLASH_COLOR = new Color(150, 200, 255);
    private static final Color F2_COLOR       = new Color(20, 160, 140);   // teal — distinguishes from fighter 1
    private static final Color F2_FLASH_COLOR = new Color(120, 230, 210);
    private static final Color ARENA_BG       = new Color(30, 30, 30);
    private static final Color GROUND_COLOR   = new Color(60, 55, 50);

    // ── Battle engine ─────────────────────────────────────────────────────────
    private final CharacterBattle battle;

    // ── Timers ────────────────────────────────────────────────────────────────
    private javax.swing.Timer approachTimer;
    private javax.swing.Timer f1AttackTimer;
    private javax.swing.Timer f2AttackTimer;
    private javax.swing.Timer bobTimer;
    private javax.swing.Timer f1FlashTimer;
    private javax.swing.Timer f2FlashTimer;
    private javax.swing.Timer overlayFadeTimer;

    // ── Animation state ───────────────────────────────────────────────────────
    private double f1X, f2X;
    private double f1TargetX, f2TargetX;
    private int    bobTick     = 0;
    private boolean f1Flashing = false;
    private boolean f2Flashing = false;

    // ── Floating text popups ──────────────────────────────────────────────────
    private final java.util.List<FloatingText> floatingTexts = new java.util.ArrayList<>();
    private javax.swing.Timer floatTimer;

    private static class FloatingText {
        static final int DURATION_MS = 500;
        static final int TICK_MS     = 16;
        static final int RISE_PX     = 30;
        static final int SIDE_OFFSET = 8;

        String  text;
        float   x, y;
        int     alpha;
        boolean isCrit, isHeal, isPassiveDmg, isMiss, leftAnchored;
        int     ticksLeft;
        float   dy;
        int     dAlpha;

        FloatingText(String text, float x, float y,
                     boolean isCrit, boolean isHeal, boolean isPassiveDmg,
                     boolean isMiss, boolean leftAnchored) {
            this.text = text; this.x = x; this.y = y;
            this.alpha = 255;
            this.isCrit = isCrit; this.isHeal = isHeal;
            this.isPassiveDmg = isPassiveDmg; this.isMiss = isMiss;
            this.leftAnchored = leftAnchored;
            int totalTicks = DURATION_MS / TICK_MS;
            this.ticksLeft = totalTicks;
            this.dy        = (float) RISE_PX / totalTicks;
            this.dAlpha    = 255 / totalTicks;
        }

        boolean tick() {
            y -= dy; alpha -= dAlpha; ticksLeft--;
            return alpha > 0 && ticksLeft > 0;
        }
    }

    // ── End overlay ───────────────────────────────────────────────────────────
    private CharacterBattle.BattleState battleResult = null;
    private int overlayAlpha = 0;

    // ── Arena canvas ──────────────────────────────────────────────────────────
    private final ArenaCanvas arena;

    // ── UI widgets ────────────────────────────────────────────────────────────
    private JProgressBar f1HpBar, f2HpBar;
    private JLabel       f1HpLabel, f2HpLabel;
    private JTextArea    logArea;
    private JButton      startBtn, resetBtn;
    private JLabel       statusLabel;

    // ── Constructor ───────────────────────────────────────────────────────────

    public CharacterBattlePanel(CharacterBattle battle) {
        this.battle = battle;
        battle.addListener(this);

        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        arena = new ArenaCanvas();

        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.add(buildLogPanel(),     BorderLayout.CENTER);
        south.add(buildControlPanel(), BorderLayout.SOUTH);

        add(buildStatStrip(), BorderLayout.NORTH);
        add(arena,            BorderLayout.CENTER);
        add(south,            BorderLayout.SOUTH);

        resetPositions();
        refreshHpBars();
    }

    // ── UI builders ───────────────────────────────────────────────────────────

    private JPanel buildStatStrip() {
        JPanel strip = new JPanel(new GridLayout(1, 2, 20, 0));
        strip.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        Character f1 = battle.getFighter1();
        Character f2 = battle.getFighter2();

        // Fighter 1 side
        JPanel p1 = new JPanel(new BorderLayout(4, 2));
        f1HpBar   = makeHpBar(f1.getMaxHp());
        f1HpLabel = new JLabel(hpText(f1));
        JLabel n1 = new JLabel(f1.getName() + "  [" + f1.getClan() + " Clan]");
        n1.setFont(n1.getFont().deriveFont(Font.BOLD, 13f));
        p1.add(n1,        BorderLayout.NORTH);
        p1.add(f1HpBar,   BorderLayout.CENTER);
        p1.add(f1HpLabel, BorderLayout.SOUTH);

        // Fighter 2 side
        JPanel p2 = new JPanel(new BorderLayout(4, 2));
        f2HpBar   = makeHpBar(f2.getMaxHp());
        f2HpLabel = new JLabel(hpText(f2), SwingConstants.RIGHT);
        JLabel n2 = new JLabel(f2.getName() + "  [" + f2.getClan() + " Clan]", SwingConstants.RIGHT);
        n2.setFont(n2.getFont().deriveFont(Font.BOLD, 13f));
        p2.add(n2,        BorderLayout.NORTH);
        p2.add(f2HpBar,   BorderLayout.CENTER);
        p2.add(f2HpLabel, BorderLayout.SOUTH);

        strip.add(p1);
        strip.add(p2);
        return strip;
    }

    private JProgressBar makeHpBar(int max) {
        JProgressBar bar = new JProgressBar(0, max);
        bar.setValue(max);
        bar.setStringPainted(false);
        bar.setForeground(new Color(60, 180, 60));
        bar.setPreferredSize(new Dimension(0, 14));
        return bar;
    }

    private JScrollPane buildLogPanel() {
        logArea = new JTextArea(6, 50);
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane sp = new JScrollPane(logArea);
        sp.setBorder(BorderFactory.createTitledBorder("Battle Log"));
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        sp.setPreferredSize(new Dimension(0, 120));
        return sp;
    }

    private JPanel buildControlPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        startBtn    = new JButton("Start Battle");
        resetBtn    = new JButton("Reset");
        statusLabel = new JLabel("Press Start to begin.");
        startBtn.addActionListener(this::onStart);
        resetBtn.addActionListener(this::onReset);
        p.add(startBtn); p.add(resetBtn); p.add(statusLabel);
        return p;
    }

    // ── Control handlers ──────────────────────────────────────────────────────

    private void onStart(ActionEvent e) {
        if (battle.getState() == CharacterBattle.BattleState.ONGOING ||
                battle.getState() == CharacterBattle.BattleState.APPROACHING) return;

        logArea.setText("");
        battle.start();
        appendLog("=== Character Battle Start! ===");
        appendLog(battle.getFighter1().getName() + " [" + battle.getFighter1().getClan() + "] vs "
                + battle.getFighter2().getName() + " [" + battle.getFighter2().getClan() + "]");

        startBtn.setEnabled(false);
        statusLabel.setText("Approaching…");
        startApproach();
    }

    private void onReset(ActionEvent e) {
        battle.stop();
        stopAllTimers();
        battle.getFighter1().reset();
        battle.getFighter2().reset();
        logArea.setText("");
        startBtn.setEnabled(true);
        statusLabel.setText("Press Start to begin.");
        f1Flashing = false;
        f2Flashing = false;
        floatingTexts.clear();
        battleResult = null;
        overlayAlpha = 0;
        resetPositions();
        refreshHpBars();
        arena.repaint();
    }

    // ── Approach animation ────────────────────────────────────────────────────

    private void resetPositions() {
        f1X = -FIGHTER_SIZE;
        f2X = 9999;
    }

    private void startApproach() {
        int w   = arena.getWidth();
        int mid = w / 2;
        f1TargetX = mid - FIGHTER_SIZE - ENGAGE_GAP / 2.0;
        f2TargetX = mid + ENGAGE_GAP / 2.0;
        f1X = -FIGHTER_SIZE;
        f2X = w;

        approachTimer = new javax.swing.Timer(APPROACH_STEP_MS, ev -> {
            boolean d1 = false, d2 = false;
            if (f1X < f1TargetX) f1X = Math.min(f1X + APPROACH_SPEED, f1TargetX); else d1 = true;
            if (f2X > f2TargetX) f2X = Math.max(f2X - APPROACH_SPEED, f2TargetX); else d2 = true;
            arena.repaint();
            if (d1 && d2) {
                approachTimer.stop();
                battle.setEngaged();
                statusLabel.setText("Fighting…");
                startCombatTimers();
                startBobTimer();
            }
        });
        approachTimer.start();
    }

    // ── Combat timers ─────────────────────────────────────────────────────────

    private void startCombatTimers() {
        f1AttackTimer = new javax.swing.Timer(battle.getFighter1().getAttackSpeed(), ev -> {
            battle.fighter1Tick();
            refreshHpBars();
        });
        f1AttackTimer.setInitialDelay(0);
        f1AttackTimer.start();

        f2AttackTimer = new javax.swing.Timer(battle.getFighter2().getAttackSpeed(), ev -> {
            battle.fighter2Tick();
            refreshHpBars();
        });
        f2AttackTimer.setInitialDelay(0);
        f2AttackTimer.start();
    }

    private void startBobTimer() {
        bobTimer = new javax.swing.Timer(APPROACH_STEP_MS, ev -> { bobTick++; arena.repaint(); });
        bobTimer.start();
    }

    // ── CharacterBattle.BattleListener ───────────────────────────────────────

    @Override
    public void onFighter1Attack(String logEntry, int damage, boolean isCrit, boolean isMiss) {
        SwingUtilities.invokeLater(() -> {
            appendLog(logEntry);
            if (isMiss) spawnMissPopup(false);
            else { flashF2(); showDamagePopup(false, damage, isCrit); }
            refreshHpBars();
        });
    }

    @Override
    public void onFighter2Attack(String logEntry, int damage, boolean isCrit, boolean isMiss) {
        SwingUtilities.invokeLater(() -> {
            appendLog(logEntry);
            if (isMiss) spawnMissPopup(true);
            else { flashF1(); showDamagePopup(true, damage, isCrit); }
            refreshHpBars();
        });
    }

    @Override
    public void onPassive(String logEntry, Entity owner, int amount, boolean isHeal) {
        SwingUtilities.invokeLater(() -> {
            appendLog(logEntry);
            boolean onF1 = isHeal
                    ? (owner == battle.getFighter1())
                    : (owner != battle.getFighter1());
            showPassivePopup(onF1, amount, isHeal);
            refreshHpBars();
        });
    }

    @Override
    public void onBattleEnd(CharacterBattle.BattleState result) {
        SwingUtilities.invokeLater(() -> {
            stopAllTimers();
            String msg = switch (result) {
                case FIGHTER1_WIN -> battle.getFighter1().getName() + " wins!";
                case FIGHTER2_WIN -> battle.getFighter2().getName() + " wins!";
                default           -> "Battle over.";
            };
            appendLog("=== " + msg + " ===");
            statusLabel.setText(msg);
            refreshHpBars();

            new javax.swing.Timer(400, e -> {
                ((javax.swing.Timer) e.getSource()).stop();
                battleResult = result;
                overlayAlpha = 0;
                overlayFadeTimer = new javax.swing.Timer(16, ev -> {
                    overlayAlpha = Math.min(overlayAlpha + 8, 180);
                    arena.repaint();
                    if (overlayAlpha >= 180) overlayFadeTimer.stop();
                });
                overlayFadeTimer.start();
            }) {{ setRepeats(false); start(); }};
        });
    }

    // ── Flash helpers ─────────────────────────────────────────────────────────

    private void flashF1() {
        f1Flashing = true; arena.repaint();
        if (f1FlashTimer != null) f1FlashTimer.stop();
        f1FlashTimer = new javax.swing.Timer(HIT_FLASH_MS, ev -> {
            f1Flashing = false; arena.repaint(); f1FlashTimer.stop();
        });
        f1FlashTimer.start();
    }

    private void flashF2() {
        f2Flashing = true; arena.repaint();
        if (f2FlashTimer != null) f2FlashTimer.stop();
        f2FlashTimer = new javax.swing.Timer(HIT_FLASH_MS, ev -> {
            f2Flashing = false; arena.repaint(); f2FlashTimer.stop();
        });
        f2FlashTimer.start();
    }

    // ── Popup helpers ─────────────────────────────────────────────────────────

    private void showDamagePopup(boolean onF1, int dmg, boolean isCrit) {
        spawnPopup(onF1, "-" + dmg + (isCrit ? "!!" : ""), isCrit, false, false);
    }

    private void showPassivePopup(boolean onF1, int amount, boolean isHeal) {
        spawnPopup(onF1, (isHeal ? "+" : "-") + amount, false, isHeal, !isHeal);
    }

    private void spawnMissPopup(boolean onF1) {
        int   squareY = getSquareY();
        float spawnX  = onF1
                ? (float) f1X + FIGHTER_SIZE / 2f
                : (float) f2X + FIGHTER_SIZE / 2f;
        floatingTexts.add(new FloatingText("MISS!", spawnX, squareY - 20,
                false, false, false, true, false));
        ensureFloatTimer();
    }

    private void spawnPopup(boolean onF1, String text,
                            boolean isCrit, boolean isHeal, boolean isPassiveDmg) {
        int squareY = getSquareY();
        float spawnX; boolean leftAnchored; float spawnY;

        if (isHeal) {
            if (onF1) {
                spawnX = (float) f1X - FloatingText.SIDE_OFFSET;
                leftAnchored = false;
            } else {
                spawnX = (float) f2X + FIGHTER_SIZE + FloatingText.SIDE_OFFSET;
                leftAnchored = true;
            }
            spawnY = squareY + FIGHTER_SIZE / 2f;
        } else {
            spawnX = onF1
                    ? (float) f1X + FIGHTER_SIZE / 2f
                    : (float) f2X + FIGHTER_SIZE / 2f;
            spawnY = squareY - 20;
            leftAnchored = false;
        }

        floatingTexts.add(new FloatingText(text, spawnX, spawnY,
                isCrit, isHeal, isPassiveDmg, false, leftAnchored));
        ensureFloatTimer();
    }

    private void ensureFloatTimer() {
        if (floatTimer != null && floatTimer.isRunning()) return;
        floatTimer = new javax.swing.Timer(FloatingText.TICK_MS, ev -> {
            floatingTexts.removeIf(ft -> !ft.tick());
            arena.repaint();
            if (floatingTexts.isEmpty()) floatTimer.stop();
        });
        floatTimer.start();
    }

    private int getSquareY() {
        int h = arena.getHeight();
        return (int)(h * 0.78) - FIGHTER_SIZE;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void stopAllTimers() {
        if (approachTimer    != null) approachTimer.stop();
        if (f1AttackTimer    != null) f1AttackTimer.stop();
        if (f2AttackTimer    != null) f2AttackTimer.stop();
        if (bobTimer         != null) bobTimer.stop();
        if (f1FlashTimer     != null) f1FlashTimer.stop();
        if (f2FlashTimer     != null) f2FlashTimer.stop();
        if (floatTimer       != null) floatTimer.stop();
        if (overlayFadeTimer != null) overlayFadeTimer.stop();
        floatingTexts.clear();
    }

    private void refreshHpBars() {
        Character f1 = battle.getFighter1();
        Character f2 = battle.getFighter2();

        f1HpBar.setValue(f1.getCurrentHp());
        f1HpLabel.setText(hpText(f1));
        updateBarColor(f1HpBar, f1.getHpPercent());

        f2HpBar.setValue(f2.getCurrentHp());
        f2HpLabel.setText(hpText(f2));
        updateBarColor(f2HpBar, f2.getHpPercent());
    }

    private void updateBarColor(JProgressBar bar, double pct) {
        if      (pct > 0.5)  bar.setForeground(new Color(60, 180, 60));
        else if (pct > 0.25) bar.setForeground(new Color(220, 180, 0));
        else                 bar.setForeground(new Color(200, 40, 40));
    }

    private void appendLog(String text) {
        logArea.append(text + "\n");
        String[] lines = logArea.getText().split("\n");
        if (lines.length > LOG_LIMIT) {
            StringBuilder sb = new StringBuilder();
            for (int i = lines.length - LOG_LIMIT; i < lines.length; i++)
                sb.append(lines[i]).append("\n");
            logArea.setText(sb.toString());
        }
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private String hpText(Entity e) { return e.getCurrentHp() + " / " + e.getMaxHp() + " HP"; }

    // ═════════════════════════════════════════════════════════════════════════
    //  Inner class: Arena Canvas
    // ═════════════════════════════════════════════════════════════════════════

    private class ArenaCanvas extends JPanel {

        ArenaCanvas() {
            setPreferredSize(new Dimension(600, 200));
            setBackground(ARENA_BG);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int groundY = (int)(h * 0.78);
            int squareY = groundY - FIGHTER_SIZE;

            // Ground
            g2.setColor(GROUND_COLOR);
            g2.fillRect(0, groundY, w, h - groundY);

            // Centre line
            g2.setColor(new Color(80, 80, 80));
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    0, new float[]{4, 4}, 0));
            g2.drawLine(w / 2, 0, w / 2, groundY);
            g2.setStroke(new BasicStroke(1));

            boolean engaged = battle.getState() == CharacterBattle.BattleState.ONGOING
                    || battle.getState() == CharacterBattle.BattleState.FIGHTER1_WIN
                    || battle.getState() == CharacterBattle.BattleState.FIGHTER2_WIN;

            int b1 = engaged ? (int)(Math.sin(bobTick * 0.18) * 3)            : 0;
            int b2 = engaged ? (int)(Math.sin(bobTick * 0.18 + Math.PI) * 3)  : 0;

            // Fighter 1
            int x1 = (int) f1X, y1 = squareY + b1;
            Color c1 = f1Flashing ? F1_FLASH_COLOR : F1_COLOR;
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillOval(x1 + 4, groundY - 4, FIGHTER_SIZE - 8, 8);
            g2.setColor(c1);
            g2.fillRoundRect(x1, y1, FIGHTER_SIZE, FIGHTER_SIZE, 8, 8);
            g2.setColor(c1.darker());
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x1, y1, FIGHTER_SIZE, FIGHTER_SIZE, 8, 8);
            g2.setStroke(new BasicStroke(1));
            drawCenteredLabel(g2, battle.getFighter1().getName(), x1 + FIGHTER_SIZE / 2, y1 - 6, Color.WHITE);

            // Fighter 2
            int x2 = (int) f2X, y2 = squareY + b2;
            Color c2 = f2Flashing ? F2_FLASH_COLOR : F2_COLOR;
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillOval(x2 + 4, groundY - 4, FIGHTER_SIZE - 8, 8);
            g2.setColor(c2);
            g2.fillRoundRect(x2, y2, FIGHTER_SIZE, FIGHTER_SIZE, 8, 8);
            g2.setColor(c2.darker());
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x2, y2, FIGHTER_SIZE, FIGHTER_SIZE, 8, 8);
            g2.setStroke(new BasicStroke(1));
            drawCenteredLabel(g2, battle.getFighter2().getName(), x2 + FIGHTER_SIZE / 2, y2 - 6, Color.WHITE);

            // Floating popups
            for (FloatingText ft : floatingTexts) {
                if (ft.isMiss) drawMissText(g2, ft.text, (int) ft.x, (int) ft.y, ft.alpha);
                else           drawDamageText(g2, ft.text, (int) ft.x, (int) ft.y, ft.alpha,
                        ft.isCrit, ft.isHeal, ft.isPassiveDmg, ft.leftAnchored);
            }

            // Dead X
            if (!battle.getFighter1().isAlive()) drawDeadX(g2, x1, y1);
            if (!battle.getFighter2().isAlive()) drawDeadX(g2, x2, y2);

            // Result overlay
            if (battleResult != null) drawResultOverlay(g2, w, h);

            g2.dispose();
        }

        private void drawCenteredLabel(Graphics2D g2, String text, int cx, int y, Color color) {
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);
            g2.setColor(new Color(0, 0, 0, 150));
            g2.drawString(text, cx - tw / 2 + 1, y + 1);
            g2.setColor(color);
            g2.drawString(text, cx - tw / 2, y);
        }

        private void drawDamageText(Graphics2D g2, String text, int x, int y, int alpha,
                                    boolean isCrit, boolean isHeal, boolean isPassiveDmg,
                                    boolean leftAnchored) {
            float size = isCrit ? 17f : 14f;
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, size));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);
            Color c = isHeal       ? new Color(80, 220, 80, alpha)
                    : isPassiveDmg ? new Color(80, 140, 255, alpha)
                    : isCrit       ? new Color(255, 210, 0, alpha)
                    :                new Color(255, 80, 80, alpha);
            g2.setColor(c);
            g2.drawString(text, leftAnchored ? x : x - tw / 2, y);
        }

        private void drawMissText(Graphics2D g2, String text, int cx, int y, int alpha) {
            g2.setFont(g2.getFont().deriveFont(Font.BOLD | Font.ITALIC, 14f));
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);
            g2.setColor(new Color(200, 200, 200, alpha));
            g2.drawString(text, cx - tw / 2, y);
        }

        private void drawDeadX(Graphics2D g2, int x, int y) {
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int pad = 10;
            g2.drawLine(x + pad, y + pad, x + FIGHTER_SIZE - pad, y + FIGHTER_SIZE - pad);
            g2.drawLine(x + FIGHTER_SIZE - pad, y + pad, x + pad, y + FIGHTER_SIZE - pad);
            g2.setStroke(new BasicStroke(1));
        }

        private void drawResultOverlay(Graphics2D g2, int w, int h) {
            boolean f1Won = battleResult == CharacterBattle.BattleState.FIGHTER1_WIN;

            g2.setColor(new Color(0, 0, 0, overlayAlpha));
            g2.fillRect(0, 0, w, h);

            Color bannerColor = f1Won
                    ? new Color(20, 80, 160, overlayAlpha)
                    : new Color(10, 110, 90, overlayAlpha);
            int bannerH = h / 3, bannerY = h / 2 - h / 6;
            g2.setColor(bannerColor);
            g2.fillRoundRect(w / 6, bannerY, w * 2 / 3, bannerH, 16, 16);
            g2.setColor(new Color(255, 255, 255, Math.min(overlayAlpha + 40, 255)));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(w / 6, bannerY, w * 2 / 3, bannerH, 16, 16);
            g2.setStroke(new BasicStroke(1));

            String headline = "VICTORY";
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 36f));
            FontMetrics fmH = g2.getFontMetrics();
            int headW = fmH.stringWidth(headline);
            int headY = bannerY + bannerH / 2 - 4;
            g2.setColor(new Color(0, 0, 0, overlayAlpha));
            g2.drawString(headline, w / 2 - headW / 2 + 2, headY + 2);
            g2.setColor(new Color(180, 220, 255, overlayAlpha));
            g2.drawString(headline, w / 2 - headW / 2, headY);

            String winner = f1Won ? battle.getFighter1().getName() : battle.getFighter2().getName();
            String loser  = f1Won ? battle.getFighter2().getName() : battle.getFighter1().getName();
            String sub = winner + " defeated " + loser + "!";
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 13f));
            FontMetrics fmS = g2.getFontMetrics();
            int subW = fmS.stringWidth(sub);
            g2.setColor(new Color(220, 220, 220, overlayAlpha));
            g2.drawString(sub, w / 2 - subW / 2, headY + fmH.getHeight() - 4);

            String prompt = "Press Reset to play again";
            g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 11f));
            FontMetrics fmP = g2.getFontMetrics();
            int promptW = fmP.stringWidth(prompt);
            g2.setColor(new Color(160, 160, 160, overlayAlpha));
            g2.drawString(prompt, w / 2 - promptW / 2, bannerY + bannerH - 10);
        }
    }
}
