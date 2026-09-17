package Combat.NormalBattle;

import Entities.Entity;
import Entities.Enemy;
import Entities.Character;
import Combat.NormalBattle.Battle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * BattlePanel — Swing GUI for Ninja Warz 1v1 combat.
 *
 *  Layout
 *  ┌─────────────────────────────────────────┐
 *  │  [Player HP bar]      [Enemy HP bar]    │  ← stat strip
 *  ├─────────────────────────────────────────┤
 *  │                                         │
 *  │          A r e n a   C a n v a s       │  ← animated squares
 *  │                                         │
 *  ├─────────────────────────────────────────┤
 *  │  Battle Log (scrollable text)           │
 *  ├─────────────────────────────────────────┤
 *  │  [Start]  [Reset]   status              │
 *  └─────────────────────────────────────────┘
 */
public class BattlePanel extends JPanel implements Battle.BattleListener {

    // ── Timing ────────────────────────────────────────────────────────────────
    private static final int APPROACH_STEP_MS  = 16;    // ~60 fps approach animation
    private static final int APPROACH_SPEED    = 4;     // pixels per frame
    private static final int FIGHTER_SIZE      = 48;    // square side length
    private static final int ENGAGE_GAP        = 10;    // pixels between squares when fighting
    private static final int HIT_FLASH_MS      = 120;   // duration of hit-flash per hit
    private static final int LOG_LIMIT         = 200;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final Color PLAYER_COLOR       = new Color(40, 100, 220);
    private static final Color PLAYER_FLASH_COLOR = new Color(150, 200, 255);
    private static final Color ENEMY_COLOR        = new Color(210, 40, 40);
    private static final Color ENEMY_FLASH_COLOR  = new Color(255, 160, 160);
    private static final Color ARENA_BG           = new Color(30, 30, 30);
    private static final Color GROUND_COLOR       = new Color(60, 55, 50);

    // ── Battle engine ─────────────────────────────────────────────────────────
    private final Battle battle;

    // ── Timers ────────────────────────────────────────────────────────────────
    private javax.swing.Timer approachTimer;
    private javax.swing.Timer playerAttackTimer;
    private javax.swing.Timer enemyAttackTimer;

    // ── Animation state ───────────────────────────────────────────────────────
    private double playerX;   // current X of player square (left edge)
    private double enemyX;    // current X of enemy square (left edge)
    private double playerTargetX;
    private double enemyTargetX;

    // bob offset for idle fighting animation
    private int    bobTick       = 0;
    private javax.swing.Timer bobTimer;

    // flash state
    private boolean playerFlashing = false;
    private boolean enemyFlashing  = false;
    private javax.swing.Timer playerFlashTimer;
    private javax.swing.Timer enemyFlashTimer;

    // ── Floating text popups ─────────────────────────────────────────────────
    // Each popup is independent — new hits never kill existing ones.
    private final java.util.List<FloatingText> floatingTexts = new java.util.ArrayList<>();
    private javax.swing.Timer floatTimer;   // single shared tick for all popups

    // ── FloatingText: one independent damage/heal number ─────────────────────
    private static class FloatingText {
        static final int DURATION_MS   = 500;   // total lifetime
        static final int TICK_MS       = 16;    // ~60 fps
        static final int RISE_PX       = 30;    // total upward travel in pixels
        static final int SIDE_OFFSET   = 8;     // horizontal offset for side-anchored (heals)

        String  text;
        float   x, y;           // current draw position
        int     alpha;          // 0–255
        boolean isCrit;
        boolean isHeal;
        boolean isPassiveDmg;
        boolean isMiss;
        boolean leftAnchored;   // true for enemy-side heals (text flows right)

        // ticks remaining and per-tick deltas
        int   ticksLeft;
        float dy;               // pixels to rise per tick
        int   dAlpha;           // alpha to subtract per tick

        FloatingText(String text, float x, float y,
                     boolean isCrit, boolean isHeal, boolean isPassiveDmg,
                     boolean isMiss, boolean leftAnchored) {
            this.text         = text;
            this.x            = x;
            this.y            = y;
            this.alpha        = 255;
            this.isCrit       = isCrit;
            this.isHeal       = isHeal;
            this.isPassiveDmg = isPassiveDmg;
            this.isMiss       = isMiss;
            this.leftAnchored = leftAnchored;

            int totalTicks = DURATION_MS / TICK_MS;
            this.ticksLeft  = totalTicks;
            this.dy         = (float) RISE_PX / totalTicks;
            this.dAlpha     = 255 / totalTicks;
        }

        /** Advance one tick. Returns false when this popup should be removed. */
        boolean tick() {
            y       -= dy;
            alpha   -= dAlpha;
            ticksLeft--;
            return alpha > 0 && ticksLeft > 0;
        }
    }

    // ── Arena canvas ──────────────────────────────────────────────────────────
    private final ArenaCanvas arena;

    // ── End-of-battle overlay ─────────────────────────────────────────────────
    private Battle.BattleState battleResult = null;   // null = no overlay
    private int  overlayAlpha  = 0;                   // 0–180 (semi-transparent)
    private javax.swing.Timer overlayFadeTimer;

    // ── UI widgets ────────────────────────────────────────────────────────────
    private JProgressBar playerHpBar, enemyHpBar;
    private JLabel       playerHpLabel, enemyHpLabel;
    private JTextArea    logArea;
    private JButton      startBtn, resetBtn;
    private JLabel       statusLabel;

    // ── Constructor ───────────────────────────────────────────────────────────

    public BattlePanel(Battle battle) {
        this.battle = battle;
        battle.addListener(this);

        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        arena = new ArenaCanvas();

        add(buildStatStrip(), BorderLayout.NORTH);
        add(arena,            BorderLayout.CENTER);
        add(buildLogPanel(),  BorderLayout.SOUTH);

        // wrap log + controls in a south panel
        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.add(buildLogPanel(),     BorderLayout.CENTER);
        south.add(buildControlPanel(), BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        resetPositions();
        refreshHpBars();
    }

    // ── UI builders ───────────────────────────────────────────────────────────

    private JPanel buildStatStrip() {
        JPanel strip = new JPanel(new GridLayout(1, 2, 20, 0));
        strip.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        // Player side
        JPanel pSide = new JPanel(new BorderLayout(4, 2));
        playerHpBar   = makeHpBar(battle.getPlayer().getMaxHp());
        playerHpLabel = new JLabel(hpText(battle.getPlayer()));
        JLabel pName  = new JLabel(battle.getPlayer().getName() + "  [" + ((Character) battle.getPlayer()).getClan() + " Clan]");
        pName.setFont(pName.getFont().deriveFont(Font.BOLD, 13f));
        pSide.add(pName,          BorderLayout.NORTH);
        pSide.add(playerHpBar,    BorderLayout.CENTER);
        pSide.add(playerHpLabel,  BorderLayout.SOUTH);

        // Enemy side
        JPanel eSide = new JPanel(new BorderLayout(4, 2));
        enemyHpBar   = makeHpBar(battle.getEnemy().getMaxHp());
        enemyHpLabel = new JLabel(hpText(battle.getEnemy()), SwingConstants.RIGHT);
        JLabel eName = new JLabel(battle.getEnemy().getName() + "  [" + ((Enemy) battle.getEnemy()).getRank() + "]", SwingConstants.RIGHT);
        eName.setFont(eName.getFont().deriveFont(Font.BOLD, 13f));
        eSide.add(eName,         BorderLayout.NORTH);
        eSide.add(enemyHpBar,    BorderLayout.CENTER);
        eSide.add(enemyHpLabel,  BorderLayout.SOUTH);

        strip.add(pSide);
        strip.add(eSide);
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
        if (battle.getState() == Battle.BattleState.ONGOING ||
                battle.getState() == Battle.BattleState.APPROACHING) return;

        logArea.setText("");
        battle.start();
        appendLog("=== Battle Start! ===");

        if (battle.getEnemy() instanceof Enemy en)
            appendLog(en.getName() + ": \"" + en.getTaunt() + "\"");

        startBtn.setEnabled(false);
        statusLabel.setText("Approaching…");

        startApproach();
    }

    private void onReset(ActionEvent e) {
        battle.stop();
        stopAllTimers();
        battle.getPlayer().reset();
        battle.getEnemy().reset();
        logArea.setText("");
        startBtn.setEnabled(true);
        statusLabel.setText("Press Start to begin.");
        playerFlashing = false;
        enemyFlashing  = false;
        floatingTexts.clear();
        battleResult = null;
        overlayAlpha = 0;
        resetPositions();
        refreshHpBars();
        arena.repaint();
    }

    // ── Approach animation ────────────────────────────────────────────────────

    private void resetPositions() {
        // Will be computed relative to arena size; use placeholder until painted
        playerX = -FIGHTER_SIZE;   // off-screen left
        enemyX  = 9999;            // off-screen right (corrected in approach)
    }

    private void startApproach() {
        // Compute targets: player left of centre, enemy right of centre
        int w = arena.getWidth();
        int mid = w / 2;
        playerTargetX = mid - FIGHTER_SIZE - ENGAGE_GAP / 2.0;
        enemyTargetX  = mid + ENGAGE_GAP / 2.0;

        // Start positions: outside the canvas
        playerX = -FIGHTER_SIZE;
        enemyX  = w;

        approachTimer = new javax.swing.Timer(APPROACH_STEP_MS, ev -> {
            boolean pDone = false, eDone = false;

            if (playerX < playerTargetX) {
                playerX = Math.min(playerX + APPROACH_SPEED, playerTargetX);
            } else pDone = true;

            if (enemyX > enemyTargetX) {
                enemyX = Math.max(enemyX - APPROACH_SPEED, enemyTargetX);
            } else eDone = true;

            arena.repaint();

            if (pDone && eDone) {
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
        playerAttackTimer = new javax.swing.Timer(battle.getPlayer().getAttackSpeed(), ev -> {
            battle.playerTick();
            refreshHpBars();
        });
        playerAttackTimer.setInitialDelay(0);
        playerAttackTimer.start();

        enemyAttackTimer = new javax.swing.Timer(battle.getEnemy().getAttackSpeed(), ev -> {
            battle.enemyTick();
            refreshHpBars();
        });
        enemyAttackTimer.setInitialDelay(0);
        enemyAttackTimer.start();
    }

    private void startBobTimer() {
        bobTimer = new javax.swing.Timer(APPROACH_STEP_MS, ev -> {
            bobTick++;
            arena.repaint();
        });
        bobTimer.start();
    }

    // ── Battle.BattleListener ─────────────────────────────────────────────────

    @Override
    public void onPlayerAttack(String logEntry, int damage, boolean isCrit, boolean isMiss) {
        SwingUtilities.invokeLater(() -> {
            appendLog(logEntry);
            if (isMiss) {
                spawnMissPopup(false);
            } else {
                flashEnemy();
                showDamageDisplay(false, damage, isCrit);
            }
            refreshHpBars();
        });
    }

    @Override
    public void onEnemyAttack(String logEntry, int damage, boolean isCrit, boolean isMiss) {
        SwingUtilities.invokeLater(() -> {
            appendLog(logEntry);
            if (isMiss) {
                spawnMissPopup(true);
            } else {
                flashPlayer();
                showDamageDisplay(true, damage, isCrit);
            }
            refreshHpBars();
        });
    }

    @Override
    public void onPassive(String logEntry, Entity owner, int amount, boolean isHeal) {
        SwingUtilities.invokeLater(() -> {
            appendLog(logEntry);
            // Damage passive: popup on the opponent of the owner
            // Heal passive:   popup on the owner themselves
            boolean onPlayer = isHeal
                    ? (owner == battle.getPlayer())
                    : (owner != battle.getPlayer());
            showPassivePopup(onPlayer, amount, isHeal);
            refreshHpBars();
        });
    }

    @Override
    public void onBattleEnd(Battle.BattleState result) {
        SwingUtilities.invokeLater(() -> {
            stopAllTimers();
            String msg = switch (result) {
                case PLAYER_WIN -> battle.getPlayer().getName() + " wins!";
                case ENEMY_WIN  -> battle.getEnemy().getName()  + " wins!";
                default         -> "Battle over.";
            };
            appendLog("=== " + msg + " ===");
            statusLabel.setText(msg);
            refreshHpBars();

            // Brief pause, then fade in the result overlay
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

    private void flashPlayer() {
        playerFlashing = true;
        arena.repaint();
        if (playerFlashTimer != null) playerFlashTimer.stop();
        playerFlashTimer = new javax.swing.Timer(HIT_FLASH_MS, ev -> {
            playerFlashing = false;
            arena.repaint();
            playerFlashTimer.stop();
        });
        playerFlashTimer.start();
    }

    private void flashEnemy() {
        enemyFlashing = true;
        arena.repaint();
        if (enemyFlashTimer != null) enemyFlashTimer.stop();
        enemyFlashTimer = new javax.swing.Timer(HIT_FLASH_MS, ev -> {
            enemyFlashing = false;
            arena.repaint();
            enemyFlashTimer.stop();
        });
        enemyFlashTimer.start();
    }

    // ── Damage popup helpers ──────────────────────────────────────────────────

    /**
     * Spawns a floating text popup that rises and fades independently.
     * Multiple popups can coexist without cancelling each other.
     *
     * @param onPlayer    true = above/beside the player square; false = enemy square
     * @param text        the string to display (e.g. "-18", "+20", "-20!")
     * @param isCrit      gold colour and larger font
     * @param isHeal      green colour; positioned beside entity rather than above
     * @param isPassiveDmg blue colour (passive bypass damage)
     */
    private void spawnPopup(boolean onPlayer, String text,
                            boolean isCrit, boolean isHeal, boolean isPassiveDmg) {
        int squareY   = getSquareY();
        float spawnX, spawnY;
        boolean leftAnchored;

        if (isHeal) {
            // Heals appear beside the entity
            if (onPlayer) {
                // Player is on the left — text floats to the left of the square
                spawnX       = (float) playerX - FloatingText.SIDE_OFFSET;
                leftAnchored = false;   // right-edge anchored (text drawn ending at x)
            } else {
                // Enemy is on the right — text floats to the right of the square
                spawnX       = (float) enemyX + FIGHTER_SIZE + FloatingText.SIDE_OFFSET;
                leftAnchored = true;
            }
            spawnY = squareY + FIGHTER_SIZE / 2f;
        } else {
            // Damage appears above the entity, horizontally centred
            spawnX       = onPlayer
                    ? (float) playerX + FIGHTER_SIZE / 2f
                    : (float) enemyX  + FIGHTER_SIZE / 2f;
            spawnY       = squareY - 20;
            leftAnchored = false;
        }

        FloatingText ft = new FloatingText(text, spawnX, spawnY,
                isCrit, isHeal, isPassiveDmg, false, leftAnchored);
        floatingTexts.add(ft);
        ensureFloatTimer();
    }

    private void showDamageDisplay(boolean onPlayer, int dmg, boolean isCrit) {
        spawnPopup(onPlayer, "-" + dmg + (isCrit ? "!!" : ""), isCrit, false, false);
    }

    private void showPassivePopup(boolean onPlayer, int amount, boolean isHeal) {
        spawnPopup(onPlayer, (isHeal ? "+" : "-") + amount, false, isHeal, !isHeal);
    }

    private void spawnMissPopup(boolean onPlayer) {
        int squareY = getSquareY();
        float spawnX = onPlayer
                ? (float) playerX + FIGHTER_SIZE / 2f
                : (float) enemyX  + FIGHTER_SIZE / 2f;
        float spawnY = squareY - 20;
        FloatingText ft = new FloatingText("MISS!", spawnX, spawnY,
                false, false, false, true, false);
        floatingTexts.add(ft);
        ensureFloatTimer();
    }

    /** Lazily starts (or keeps alive) the shared float animation timer. */
    private void ensureFloatTimer() {
        if (floatTimer != null && floatTimer.isRunning()) return;
        floatTimer = new javax.swing.Timer(FloatingText.TICK_MS, ev -> {
            floatingTexts.removeIf(ft -> !ft.tick());
            arena.repaint();
            if (floatingTexts.isEmpty()) floatTimer.stop();
        });
        floatTimer.start();
    }

    /** Returns the current top-Y of the fighter squares (used for popup spawn positions). */
    private int getSquareY() {
        int h = arena.getHeight();
        int groundY = (int)(h * 0.78);
        return groundY - FIGHTER_SIZE;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void stopAllTimers() {
        if (approachTimer     != null) approachTimer.stop();
        if (playerAttackTimer != null) playerAttackTimer.stop();
        if (enemyAttackTimer  != null) enemyAttackTimer.stop();
        if (bobTimer          != null) bobTimer.stop();
        if (playerFlashTimer  != null) playerFlashTimer.stop();
        if (enemyFlashTimer   != null) enemyFlashTimer.stop();
        if (floatTimer        != null) floatTimer.stop();
        if (overlayFadeTimer  != null) overlayFadeTimer.stop();
        floatingTexts.clear();
    }

    private void refreshHpBars() {
        Entity p = battle.getPlayer();
        Entity e = battle.getEnemy();

        playerHpBar.setValue(p.getCurrentHp());
        playerHpLabel.setText(hpText(p));
        updateBarColor(playerHpBar, p.getHpPercent());

        enemyHpBar.setValue(e.getCurrentHp());
        enemyHpLabel.setText(hpText(e));
        updateBarColor(enemyHpBar, e.getHpPercent());
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

    private String hpText(Entity e) {
        return e.getCurrentHp() + " / " + e.getMaxHp() + " HP";
    }

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
            int groundY = (int)(h * 0.78);   // ground line
            int squareY = groundY - FIGHTER_SIZE;  // top of squares

            // ── Ground ────────────────────────────────────────────────────────
            g2.setColor(GROUND_COLOR);
            g2.fillRect(0, groundY, w, h - groundY);

            // ── Centre line (subtle) ──────────────────────────────────────────
            g2.setColor(new Color(80, 80, 80));
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    0, new float[]{4, 4}, 0));
            g2.drawLine(w / 2, 0, w / 2, groundY);
            g2.setStroke(new BasicStroke(1));

            // ── Bob offset (idle fighting oscillation) ────────────────────────
            int state = battle.getState().ordinal();
            boolean engaged = battle.getState() == Battle.BattleState.ONGOING
                    || battle.getState() == Battle.BattleState.PLAYER_WIN
                    || battle.getState() == Battle.BattleState.ENEMY_WIN;

            int playerBob = engaged ? (int)(Math.sin(bobTick * 0.18) * 3) : 0;
            int enemyBob  = engaged ? (int)(Math.sin(bobTick * 0.18 + Math.PI) * 3) : 0;

            // ── Draw player square ────────────────────────────────────────────
            int px = (int) playerX;
            int py = squareY + playerBob;
            Color pColor = playerFlashing ? PLAYER_FLASH_COLOR : PLAYER_COLOR;
            // shadow
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillOval(px + 4, groundY - 4, FIGHTER_SIZE - 8, 8);
            // body
            g2.setColor(pColor);
            g2.fillRoundRect(px, py, FIGHTER_SIZE, FIGHTER_SIZE, 8, 8);
            g2.setColor(pColor.darker());
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(px, py, FIGHTER_SIZE, FIGHTER_SIZE, 8, 8);
            // label
            drawCenteredLabel(g2, battle.getPlayer().getName(), px + FIGHTER_SIZE / 2, py - 6, Color.WHITE);

            // ── Draw enemy square ─────────────────────────────────────────────
            int ex = (int) enemyX;
            int ey = squareY + enemyBob;
            Color eColor = enemyFlashing ? ENEMY_FLASH_COLOR : ENEMY_COLOR;
            // shadow
            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillOval(ex + 4, groundY - 4, FIGHTER_SIZE - 8, 8);
            // body
            g2.setColor(eColor);
            g2.fillRoundRect(ex, ey, FIGHTER_SIZE, FIGHTER_SIZE, 8, 8);
            g2.setColor(eColor.darker());
            g2.drawRoundRect(ex, ey, FIGHTER_SIZE, FIGHTER_SIZE, 8, 8);
            // label
            drawCenteredLabel(g2, battle.getEnemy().getName(), ex + FIGHTER_SIZE / 2, ey - 6, Color.WHITE);

            // ── Floating text popups ─────────────────────────────────────────
            for (FloatingText ft : floatingTexts) {
                if (ft.isMiss) {
                    drawMissText(g2, ft.text, (int) ft.x, (int) ft.y, ft.alpha);
                } else {
                    drawDamageText(g2, ft.text, (int) ft.x, (int) ft.y, ft.alpha,
                            ft.isCrit, ft.isHeal, ft.isPassiveDmg, ft.leftAnchored);
                }
            }

            // ── Dead X overlay ────────────────────────────────────────────────
            if (!battle.getPlayer().isAlive()) drawDeadX(g2, px, py);
            if (!battle.getEnemy().isAlive())  drawDeadX(g2, ex, ey);

            // ── Victory / Defeat overlay ──────────────────────────────────────
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
            Color c = isHeal        ? new Color(80, 220, 80, alpha)    // green  — heal
                    : isPassiveDmg  ? new Color(80, 140, 255, alpha)   // blue   — passive damage
                    : isCrit        ? new Color(255, 210, 0, alpha)     // gold   — crit
                    :                 new Color(255, 80, 80, alpha);    // red    — normal hit
            g2.setColor(c);
            int drawX = leftAnchored ? x : x - tw / 2;
            g2.drawString(text, drawX, y);
        }


        private void drawResultOverlay(Graphics2D g2, int w, int h) {
            boolean won = battleResult == Battle.BattleState.PLAYER_WIN;

            // ── Dark veil ─────────────────────────────────────────────────────
            g2.setColor(new Color(0, 0, 0, overlayAlpha));
            g2.fillRect(0, 0, w, h);

            // ── Coloured banner ───────────────────────────────────────────────
            Color bannerColor = won
                    ? new Color(30, 120, 30, overlayAlpha)
                    : new Color(120, 20, 20, overlayAlpha);
            int bannerH = h / 3;
            int bannerY = h / 2 - bannerH / 2;
            g2.setColor(bannerColor);
            g2.fillRoundRect(w / 6, bannerY, w * 2 / 3, bannerH, 16, 16);
            g2.setColor(new Color(255, 255, 255, Math.min(overlayAlpha + 40, 255)));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(w / 6, bannerY, w * 2 / 3, bannerH, 16, 16);
            g2.setStroke(new BasicStroke(1));

            // ── Headline ──────────────────────────────────────────────────────
            String headline = won ? "VICTORY" : "DEFEAT";
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 36f));
            FontMetrics fmH = g2.getFontMetrics();
            int headW = fmH.stringWidth(headline);
            int headY = bannerY + bannerH / 2 - 4;
            // shadow
            g2.setColor(new Color(0, 0, 0, overlayAlpha));
            g2.drawString(headline, w / 2 - headW / 2 + 2, headY + 2);
            // text
            Color headColor = won
                    ? new Color(180, 255, 130, overlayAlpha)
                    : new Color(255, 120, 100, overlayAlpha);
            g2.setColor(headColor);
            g2.drawString(headline, w / 2 - headW / 2, headY);

            // ── Subtitle ──────────────────────────────────────────────────────
            String sub = won
                    ? battle.getPlayer().getName() + " defeated " + battle.getEnemy().getName() + "!"
                    : battle.getEnemy().getName()  + " has defeated " + battle.getPlayer().getName() + "!";
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 13f));
            FontMetrics fmS = g2.getFontMetrics();
            int subW = fmS.stringWidth(sub);
            int subY = headY + fmH.getHeight() - 4;
            g2.setColor(new Color(220, 220, 220, overlayAlpha));
            g2.drawString(sub, w / 2 - subW / 2, subY);

            // ── Reset prompt ──────────────────────────────────────────────────
            String prompt = "Press Reset to play again";
            g2.setFont(g2.getFont().deriveFont(Font.ITALIC, 11f));
            FontMetrics fmP = g2.getFontMetrics();
            int promptW = fmP.stringWidth(prompt);
            g2.setColor(new Color(160, 160, 160, overlayAlpha));
            g2.drawString(prompt, w / 2 - promptW / 2, bannerY + bannerH - 10);
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
    }
}