package Combat.NormalBattle;

import Entities.Entity;
import Entities.Enemy;
import Entities.Character;
import Combat.NormalBattle.Battle;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/**
 * BattlePanel — full-screen battle view for Ninja Warz.
 *
 * Layout (1280 × 720, no buttons, no log)
 * ┌──────────────────────────────────────────────────────────┐
 * │  [Player name / HP bar]        [Enemy name / HP bar]    │  ← HUD strip (drawn on canvas)
 * │                                                          │
 * │          S T A G E   B A C K G R O U N D               │  ← placeholder, swap with gif/png
 * │                                                          │
 * │    [Player sprite]                  [Enemy sprite]       │  ← placeholder art, isometric view
 * │                                                          │
 * │              "Ready..."  /  "Fight!!"  overlay           │
 * └──────────────────────────────────────────────────────────┘
 *
 * Battle starts automatically:
 *   construction → 1 s "Ready…" → 0.8 s "Fight!!" → combat begins
 */
public class BattlePanel extends JPanel implements Battle.BattleListener {

    // ── Window ────────────────────────────────────────────────────────────────
    private static final int W = 1280;
    private static final int H = 720;

    // ── Fighter geometry (isometric-ish top-down) ─────────────────────────────
    // Fighters sit on a perspective "ground plane".  X moves left↔right,
    // Y moves toward/away from camera (up = further away = smaller on screen).
    // We project: screenX = worldX,  screenY = baseY - worldY * ISO_SCALE
    private static final double ISO_SCALE    = 0.55;   // depth compression
    private static final int    GROUND_BASE  = 530;    // screen Y of the "near" ground edge
    private static final int    SPRITE_W     = 72;     // placeholder sprite width
    private static final int    SPRITE_H     = 90;     // placeholder sprite height
    private static final int    ENGAGE_DIST  = 110;    // world-X gap between fighters when engaged
    private static final int    APPROACH_SPEED = 5;    // world-X pixels per frame

    // Player starts far left, enemy far right (world coords)
    private static final int PLAYER_START_X = -500;
    private static final int ENEMY_START_X  =  500;

    // Target positions when engaged (centred on screen)
    private static final int PLAYER_TARGET_X = -ENGAGE_DIST / 2;
    private static final int ENEMY_TARGET_X  =  ENGAGE_DIST / 2;

    // World Y (depth): player is slightly "nearer" (lower Y = closer to camera)
    private static final int PLAYER_WORLD_Y  = 60;
    private static final int ENEMY_WORLD_Y   = 90;

    // ── HUD ───────────────────────────────────────────────────────────────────
    private static final int HUD_H           = 70;     // height of the top HUD strip
    private static final int BAR_W           = 320;
    private static final int BAR_H           = 18;
    private static final int BAR_MARGIN      = 30;     // from screen edge

    // ── Timing ────────────────────────────────────────────────────────────────
    private static final int TICK_MS         = 16;     // ~60 fps render loop
    private static final int READY_MS        = 1200;
    private static final int FIGHT_MS        = 800;
    private static final int HIT_FLASH_MS    = 100;
    private static final int BOB_AMPLITUDE   = 3;      // px

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final Color BG_TOP        = new Color(60, 80, 50);
    private static final Color BG_BOT        = new Color(100, 120, 70);
    private static final Color GROUND_NEAR   = new Color(130, 110, 70);
    private static final Color GROUND_FAR    = new Color(80,  90, 55);
    private static final Color PLAYER_COL    = new Color(50, 120, 240);
    private static final Color PLAYER_FLASH  = new Color(180, 220, 255);
    private static final Color ENEMY_COL     = new Color(220, 50, 50);
    private static final Color ENEMY_FLASH   = new Color(255, 180, 160);
    private static final Color HUD_BG        = new Color(0, 0, 0, 160);
    private static final Color BAR_GREEN     = new Color(60, 200, 60);
    private static final Color BAR_YELLOW    = new Color(230, 190, 0);
    private static final Color BAR_RED       = new Color(210, 40, 40);
    private static final Color BAR_EMPTY     = new Color(40, 40, 40);

    // ── Battle engine ─────────────────────────────────────────────────────────
    private final Battle battle;

    // ── Render / master timer ─────────────────────────────────────────────────
    private final javax.swing.Timer renderTimer;

    // ── Intro sequence ────────────────────────────────────────────────────────
    private enum IntroState { READY, FIGHT, DONE }
    private IntroState introState   = IntroState.READY;
    private int        introAlpha   = 0;       // 0-255 fade in
    private int        introTick    = 0;       // frames elapsed in current state
    private static final int INTRO_FADE_TICKS = 12;
    private static final int READY_TICKS  = (int)((READY_MS) / (double)TICK_MS);
    private static final int FIGHT_TICKS  = (int)((FIGHT_MS) / (double)TICK_MS);

    // ── Attack timers ─────────────────────────────────────────────────────────
    private javax.swing.Timer playerAttackTimer;
    private javax.swing.Timer enemyAttackTimer;

    // ── World positions ───────────────────────────────────────────────────────
    private double playerWorldX = PLAYER_START_X;
    private double enemyWorldX  = ENEMY_START_X;
    private boolean approaching = false;   // set to true by beginCombat()
    private int     bobTick     = 0;

    // ── Flash state ───────────────────────────────────────────────────────────
    private boolean playerFlashing = false;
    private boolean enemyFlashing  = false;
    private int     playerFlashTick = 0;
    private int     enemyFlashTick  = 0;

    // ── Floating text ─────────────────────────────────────────────────────────
    private final java.util.List<FloatingText> floatingTexts = new java.util.ArrayList<>();

    private static class FloatingText {
        static final int DURATION_MS = 700;
        static final int RISE_PX     = 40;
        String  text;
        float   x, y;
        int     alpha = 255;
        boolean isCrit, isHeal, isPassiveDmg, isMiss, leftAnchored;
        int   ticksLeft;
        float dy;
        int   dAlpha;

        FloatingText(String text, float x, float y,
                     boolean isCrit, boolean isHeal, boolean isPassiveDmg,
                     boolean isMiss, boolean leftAnchored) {
            this.text = text; this.x = x; this.y = y;
            this.isCrit = isCrit; this.isHeal = isHeal;
            this.isPassiveDmg = isPassiveDmg; this.isMiss = isMiss;
            this.leftAnchored = leftAnchored;
            int total = DURATION_MS / TICK_MS;
            ticksLeft = total;
            dy     = (float) RISE_PX / total;
            dAlpha = 255 / total;
        }

        boolean tick() { y -= dy; alpha -= dAlpha; ticksLeft--; return alpha > 0 && ticksLeft > 0; }
    }

    // ── End overlay ───────────────────────────────────────────────────────────
    private Battle.BattleState battleResult = null;
    private int overlayAlpha = 0;
    private javax.swing.Timer overlayFadeTimer;

    // ── Placeholder sprite images (generated once) ────────────────────────────
    // Replace BufferedImage fields with ImageIcon / sprite sheet loads later.
    private final BufferedImage playerSprite;
    private final BufferedImage enemySprite;
    private final BufferedImage bgImage;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────────────

    public BattlePanel(Battle battle) {
        this.battle = battle;
        battle.addListener(this);

        setPreferredSize(new Dimension(W, H));
        setLayout(null);   // we paint everything manually

        playerSprite = makePlaceholderSprite(PLAYER_COL,
                battle.getPlayer().getName().substring(0, 1));
        enemySprite  = makePlaceholderSprite(ENEMY_COL,
                battle.getEnemy().getName().substring(0, 1));
        bgImage      = makePlaceholderBg();

        // Master render loop
        renderTimer = new javax.swing.Timer(TICK_MS, e -> tick());
        renderTimer.start();
    }

    // ── Master tick ───────────────────────────────────────────────────────────

    private void tick() {
        tickIntro();
        if (approaching) tickApproach();
        if (!approaching) bobTick++;

        // Flash timers
        if (playerFlashing && ++playerFlashTick > HIT_FLASH_MS / TICK_MS) {
            playerFlashing = false; playerFlashTick = 0;
        }
        if (enemyFlashing && ++enemyFlashTick > HIT_FLASH_MS / TICK_MS) {
            enemyFlashing = false; enemyFlashTick = 0;
        }

        floatingTexts.removeIf(ft -> !ft.tick());
        repaint();
    }

    // ── Intro sequence ────────────────────────────────────────────────────────

    private void tickIntro() {
        if (introState == IntroState.DONE) return;
        introTick++;

        if (introState == IntroState.READY) {
            introAlpha = Math.min(255, introTick * (255 / INTRO_FADE_TICKS));
            if (introTick >= READY_TICKS) {
                introState = IntroState.FIGHT;
                introTick  = 0;
                introAlpha = 0;
            }
        } else if (introState == IntroState.FIGHT) {
            introAlpha = Math.min(255, introTick * (255 / INTRO_FADE_TICKS));
            if (introTick >= FIGHT_TICKS) {
                introState = IntroState.DONE;
                introAlpha = 0;
                beginCombat();
            }
        }
    }

    private boolean combatStarted = false;  // true once intro finishes

    private void beginCombat() {
        battle.start();
        combatStarted = true;
        approaching   = true;   // arm the approach now that battle.start() has been called
    }

    // ── Approach ──────────────────────────────────────────────────────────────

    private void tickApproach() {
        if (!combatStarted) return;   // don't move until intro is done
        boolean pDone = false, eDone = false;
        if (playerWorldX < PLAYER_TARGET_X) playerWorldX = Math.min(playerWorldX + APPROACH_SPEED, PLAYER_TARGET_X);
        else pDone = true;
        if (enemyWorldX  > ENEMY_TARGET_X)  enemyWorldX  = Math.max(enemyWorldX  - APPROACH_SPEED, ENEMY_TARGET_X);
        else eDone = true;

        if (pDone && eDone) {
            approaching = false;
            battle.setEngaged();
            startCombatTimers();
        }
    }

    private void startCombatTimers() {
        playerAttackTimer = new javax.swing.Timer(battle.getPlayer().getAttackSpeed(), e -> {
            battle.playerTick();
        });
        playerAttackTimer.setInitialDelay(0);
        playerAttackTimer.start();

        enemyAttackTimer = new javax.swing.Timer(battle.getEnemy().getAttackSpeed(), e -> {
            battle.enemyTick();
        });
        enemyAttackTimer.setInitialDelay(0);
        enemyAttackTimer.start();
    }

    private void stopAllTimers() {
        renderTimer.stop();
        if (playerAttackTimer != null) playerAttackTimer.stop();
        if (enemyAttackTimer  != null) enemyAttackTimer.stop();
        if (overlayFadeTimer  != null) overlayFadeTimer.stop();
        battle.stop();
    }

    // ── Battle.BattleListener ─────────────────────────────────────────────────

    @Override
    public void onPlayerAttack(String log, int damage, boolean isCrit, boolean isMiss) {
        SwingUtilities.invokeLater(() -> {
            if (isMiss) spawnMissPopup(false);
            else { enemyFlashing = true; enemyFlashTick = 0; showDmgPopup(false, damage, isCrit); }
        });
    }

    @Override
    public void onEnemyAttack(String log, int damage, boolean isCrit, boolean isMiss) {
        SwingUtilities.invokeLater(() -> {
            if (isMiss) spawnMissPopup(true);
            else { playerFlashing = true; playerFlashTick = 0; showDmgPopup(true, damage, isCrit); }
        });
    }

    @Override
    public void onPassive(String log, Entity owner, int amount, boolean isHeal) {
        SwingUtilities.invokeLater(() -> {
            boolean onPlayer = isHeal ? (owner == battle.getPlayer()) : (owner != battle.getPlayer());
            spawnPassivePopup(onPlayer, amount, isHeal);
        });
    }

    @Override
    public void onBattleEnd(Battle.BattleState result) {
        SwingUtilities.invokeLater(() -> {
            if (playerAttackTimer != null) playerAttackTimer.stop();
            if (enemyAttackTimer  != null) enemyAttackTimer.stop();
            battleResult = result;
            overlayAlpha = 0;
            overlayFadeTimer = new javax.swing.Timer(TICK_MS, e -> {
                overlayAlpha = Math.min(overlayAlpha + 6, 200);
                if (overlayAlpha >= 200) overlayFadeTimer.stop();
            });
            overlayFadeTimer.start();
        });
    }

    // ── Popup helpers ─────────────────────────────────────────────────────────

    private void showDmgPopup(boolean onPlayer, int dmg, boolean isCrit) {
        Point sp = spriteScreenPos(onPlayer ? playerWorldX : enemyWorldX,
                onPlayer ? PLAYER_WORLD_Y : ENEMY_WORLD_Y);
        float sx = sp.x + SPRITE_W / 2f;
        float sy = sp.y - 10;
        String txt = "-" + dmg + (isCrit ? "!" : "");
        floatingTexts.add(new FloatingText(txt, sx, sy, isCrit, false, false, false, false));
    }

    private void spawnMissPopup(boolean onPlayer) {
        Point sp = spriteScreenPos(onPlayer ? playerWorldX : enemyWorldX,
                onPlayer ? PLAYER_WORLD_Y : ENEMY_WORLD_Y);
        floatingTexts.add(new FloatingText("MISS!", sp.x + SPRITE_W / 2f, sp.y - 10,
                false, false, false, true, false));
    }

    private void spawnPassivePopup(boolean onPlayer, int amount, boolean isHeal) {
        Point sp = spriteScreenPos(onPlayer ? playerWorldX : enemyWorldX,
                onPlayer ? PLAYER_WORLD_Y : ENEMY_WORLD_Y);
        String txt = (isHeal ? "+" : "-") + amount;
        boolean leftAnchored;
        float sx, sy;
        if (isHeal) {
            // Heal: beside the sprite
            leftAnchored = !onPlayer;  // enemy side → text goes right
            sx = onPlayer ? sp.x - 8 : sp.x + SPRITE_W + 8;
            sy = sp.y + SPRITE_H / 2f;
        } else {
            leftAnchored = false;
            sx = sp.x + SPRITE_W / 2f;
            sy = sp.y - 10;
        }
        floatingTexts.add(new FloatingText(txt, sx, sy, false, isHeal, !isHeal, false, leftAnchored));
    }

    // ── Projection helper ─────────────────────────────────────────────────────

    /** Converts world (x, depth) to screen pixel position (top-left of sprite). */
    private Point spriteScreenPos(double worldX, double worldY) {
        int sx = (int)(W / 2.0 + worldX) - SPRITE_W / 2;
        int sy = (int)(GROUND_BASE - worldY * ISO_SCALE) - SPRITE_H;
        return new Point(sx, sy);
    }

    // ── Placeholder asset generators ──────────────────────────────────────────

    /**
     * Creates a simple coloured rectangle with an initial letter as placeholder art.
     * Replace this with ImageIO.read(getClass().getResource("/res/...")) later.
     */
    private BufferedImage makePlaceholderSprite(Color base, String initial) {
        BufferedImage img = new BufferedImage(SPRITE_W, SPRITE_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Body — slightly tapered rectangle to hint at perspective
        int[] xs = { 4, SPRITE_W - 4, SPRITE_W - 10, 10 };
        int[] ys = { 0, 0, SPRITE_H, SPRITE_H };
        g.setColor(base);
        g.fillPolygon(xs, ys, 4);
        g.setColor(base.darker());
        g.setStroke(new BasicStroke(2));
        g.drawPolygon(xs, ys, 4);

        // Initial letter centred
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(255, 255, 255, 200));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(initial, (SPRITE_W - fm.stringWidth(initial)) / 2, SPRITE_H / 2 + fm.getAscent() / 2 - 4);

        g.dispose();
        return img;
    }

    /**
     * Creates a simple top-down grass/ground placeholder background.
     * Replace with:
     *   bgImage = new ImageIcon(getClass().getResource("/res/stage_bg.png")).getImage();
     */
    private BufferedImage makePlaceholderBg() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        // Sky gradient
        GradientPaint sky = new GradientPaint(0, 0, new Color(100, 130, 180),
                0, GROUND_BASE - 60, new Color(160, 185, 140));
        g.setPaint(sky);
        g.fillRect(0, 0, W, GROUND_BASE - 60);

        // Ground — perspective trapezoid
        int[] gx = { 0,   W,    W,    0 };
        int[] gy = { GROUND_BASE - 60, GROUND_BASE - 60, H, H };
        GradientPaint ground = new GradientPaint(0, GROUND_BASE - 60, GROUND_FAR,
                0, H,               GROUND_NEAR);
        g.setPaint(ground);
        g.fillPolygon(gx, gy, 4);

        // Grid lines to reinforce perspective
        g.setColor(new Color(0, 0, 0, 25));
        g.setStroke(new BasicStroke(1));
        int vp = W / 2;  // vanishing point X
        for (int i = -8; i <= 8; i++) {
            int baseX = vp + i * 90;
            g.drawLine(vp, GROUND_BASE - 60, baseX, H);
        }
        for (int row = 0; row <= 8; row++) {
            double t = row / 8.0;
            int y = (int)(GROUND_BASE - 60 + t * (H - (GROUND_BASE - 60)));
            // converging horizontal lines
            int lx = (int)(vp - (vp) * t);
            int rx = (int)(vp + (W - vp) * t);
            g.drawLine(lx, y, rx, y);
        }

        // Placeholder label
        g.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, 14));
        g.setColor(new Color(255, 255, 255, 80));
        String label = "[ Stage Background Placeholder — replace with /res/stage_bg.gif ]";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, (W - fm.stringWidth(label)) / 2, GROUND_BASE - 70);

        g.dispose();
        return img;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  paintComponent — draws everything
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        // 1. Background
        g2.drawImage(bgImage, 0, 0, null);

        // 2. Sprites
        drawSprite(g2, battle.getPlayer(), playerWorldX, PLAYER_WORLD_Y,
                playerSprite, playerFlashing ? PLAYER_FLASH : null, false);
        drawSprite(g2, battle.getEnemy(), enemyWorldX, ENEMY_WORLD_Y,
                enemySprite,  enemyFlashing  ? ENEMY_FLASH  : null, true);

        // 3. HUD
        drawHud(g2);

        // 4. Floating text
        for (FloatingText ft : floatingTexts) {
            drawFloatingText(g2, ft);
        }

        // 5. Intro overlay (Ready / Fight)
        if (introState != IntroState.DONE) drawIntroText(g2);

        // 6. Result overlay
        if (battleResult != null) drawResultOverlay(g2);
    }

    // ── Draw: sprite ──────────────────────────────────────────────────────────

    private void drawSprite(Graphics2D g2, Entity entity, double worldX, double worldY,
                            BufferedImage sprite, Color flashColor, boolean flipX) {
        Point pos = spriteScreenPos(worldX, worldY);
        int sx = pos.x, sy = pos.y;

        // Bob when engaged
        int bob = (!approaching && entity.isAlive())
                ? (int)(Math.sin(bobTick * 0.15 + (flipX ? Math.PI : 0)) * BOB_AMPLITUDE)
                : 0;
        sy += bob;

        // Shadow ellipse on the ground
        int shadowY = (int)(GROUND_BASE - worldY * ISO_SCALE);
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillOval(sx + 8, shadowY - 8, SPRITE_W - 16, 14);

        // Draw sprite (flip enemy horizontally to face left)
        if (flipX) {
            g2.drawImage(sprite, sx + SPRITE_W, sy, -SPRITE_W, SPRITE_H, null);
        } else {
            g2.drawImage(sprite, sx, sy, null);
        }

        // Flash overlay
        if (flashColor != null) {
            g2.setColor(new Color(flashColor.getRed(), flashColor.getGreen(),
                    flashColor.getBlue(), 140));
            g2.fillRect(sx, sy, SPRITE_W, SPRITE_H);
        }

        // Dead X
        if (!entity.isAlive()) {
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int pad = 14;
            g2.drawLine(sx + pad, sy + pad, sx + SPRITE_W - pad, sy + SPRITE_H - pad);
            g2.drawLine(sx + SPRITE_W - pad, sy + pad, sx + pad, sy + SPRITE_H - pad);
            g2.setStroke(new BasicStroke(1));
        }

        // Name tag below sprite
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        String name = entity.getName();
        int tw = fm.stringWidth(name);
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(sx + SPRITE_W / 2 - tw / 2 - 4, shadowY + 2, tw + 8, 14, 6, 6);
        g2.setColor(Color.WHITE);
        g2.drawString(name, sx + SPRITE_W / 2 - tw / 2, shadowY + 13);
    }

    // ── Draw: HUD ─────────────────────────────────────────────────────────────

    private void drawHud(Graphics2D g2) {
        // Semi-transparent top bar
        g2.setColor(HUD_BG);
        g2.fillRect(0, 0, W, HUD_H);

        // Player side (left)
        Entity p = battle.getPlayer();
        String pLabel = p.getName() + (p instanceof Character c ? "  [" + c.getClan() + " Clan]" : "");
        drawHudEntry(g2, pLabel, p, BAR_MARGIN, false);

        // Enemy side (right)
        Entity e = battle.getEnemy();
        String eLabel = e.getName() + (e instanceof Enemy en ? "  [" + en.getRank() + "]" : "");
        drawHudEntry(g2, eLabel, e, W - BAR_MARGIN - BAR_W, true);
    }

    private void drawHudEntry(Graphics2D g2, String label, Entity entity, int barX, boolean rightAlign) {
        int barY = 16;

        // Name
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        String name = label;
        int nx = rightAlign ? barX + BAR_W - fm.stringWidth(name) : barX;
        g2.setColor(Color.WHITE);
        g2.drawString(name, nx, barY + 11);

        // HP bar background
        int by = barY + 18;
        g2.setColor(BAR_EMPTY);
        g2.fillRoundRect(barX, by, BAR_W, BAR_H, BAR_H, BAR_H);

        // HP bar fill
        double pct = entity.getHpPercent();
        int fillW = Math.max(0, (int)(BAR_W * pct));
        Color barCol = pct > 0.5 ? BAR_GREEN : pct > 0.25 ? BAR_YELLOW : BAR_RED;
        if (fillW > 0) {
            g2.setColor(barCol);
            g2.fillRoundRect(barX, by, fillW, BAR_H, BAR_H, BAR_H);
            // Highlight gloss
            g2.setColor(new Color(255, 255, 255, 50));
            g2.fillRoundRect(barX, by, fillW, BAR_H / 2, BAR_H, BAR_H);
        }
        g2.setColor(new Color(0, 0, 0, 120));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(barX, by, BAR_W, BAR_H, BAR_H, BAR_H);
        g2.setStroke(new BasicStroke(1));

        // HP numbers
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        fm = g2.getFontMetrics();
        String hp = entity.getCurrentHp() + " / " + entity.getMaxHp();
        int hx = rightAlign ? barX + BAR_W - fm.stringWidth(hp) : barX;
        g2.setColor(new Color(220, 220, 220));
        g2.drawString(hp, hx, by + BAR_H + 13);
    }

    // ── Draw: intro text ──────────────────────────────────────────────────────

    private void drawIntroText(Graphics2D g2) {
        boolean isReady = introState == IntroState.READY;
        String  text    = isReady ? "Ready..." : "Fight!!";
        Color   col     = isReady ? new Color(230, 230, 100) : new Color(255, 80, 80);

        g2.setFont(new Font("SansSerif", Font.BOLD, 72));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (W - fm.stringWidth(text)) / 2;
        int ty = H / 2 - 20;

        // Shadow
        g2.setColor(new Color(0, 0, 0, introAlpha / 2));
        g2.drawString(text, tx + 3, ty + 3);
        // Main
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), introAlpha));
        g2.drawString(text, tx, ty);
    }

    // ── Draw: floating text ───────────────────────────────────────────────────

    private void drawFloatingText(Graphics2D g2, FloatingText ft) {
        if (ft.isMiss) {
            g2.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, 15));
            g2.setColor(new Color(200, 200, 200, ft.alpha));
        } else {
            float size = ft.isCrit ? 20f : 15f;
            g2.setFont(new Font("SansSerif", Font.BOLD, (int) size));
            Color c = ft.isHeal       ? new Color(80, 230, 80,  ft.alpha)
                    : ft.isPassiveDmg ? new Color(80, 150, 255, ft.alpha)
                    : ft.isCrit       ? new Color(255, 215, 0,  ft.alpha)
                    :                   new Color(255, 80,  80,  ft.alpha);
            g2.setColor(c);
        }
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(ft.text);
        int dx = ft.leftAnchored ? (int) ft.x : (int) ft.x - tw / 2;
        // Drop shadow
        g2.setColor(new Color(0, 0, 0, ft.alpha / 3));
        g2.drawString(ft.text, dx + 1, (int) ft.y + 1);
        // Text
        g2.setColor(ft.isMiss ? new Color(200, 200, 200, ft.alpha)
                : ft.isHeal       ? new Color(80, 230, 80,  ft.alpha)
                : ft.isPassiveDmg ? new Color(80, 150, 255, ft.alpha)
                : ft.isCrit       ? new Color(255, 215, 0,  ft.alpha)
                :                   new Color(255, 80,  80,  ft.alpha));
        g2.drawString(ft.text, dx, (int) ft.y);
    }

    // ── Draw: result overlay ──────────────────────────────────────────────────

    private void drawResultOverlay(Graphics2D g2) {
        boolean won = battleResult == Battle.BattleState.PLAYER_WIN;

        // Dark veil
        g2.setColor(new Color(0, 0, 0, overlayAlpha / 2));
        g2.fillRect(0, 0, W, H);

        int bw = 500, bh = 160;
        int bx = (W - bw) / 2, by = H / 2 - bh / 2;

        // Banner
        Color bannerCol = won ? new Color(20, 80, 30, overlayAlpha)
                : new Color(80, 20, 20, overlayAlpha);
        g2.setColor(bannerCol);
        g2.fillRoundRect(bx, by, bw, bh, 20, 20);
        g2.setColor(new Color(255, 255, 255, Math.min(overlayAlpha + 30, 255)));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(bx, by, bw, bh, 20, 20);
        g2.setStroke(new BasicStroke(1));

        // Headline
        String headline = won ? "VICTORY" : "DEFEAT";
        g2.setFont(new Font("SansSerif", Font.BOLD, 48));
        FontMetrics fmH = g2.getFontMetrics();
        int hx = bx + (bw - fmH.stringWidth(headline)) / 2;
        int hy = by + bh / 2 + 4;
        g2.setColor(new Color(0, 0, 0, overlayAlpha));
        g2.drawString(headline, hx + 3, hy + 3);
        Color headCol = won ? new Color(150, 255, 130, overlayAlpha)
                : new Color(255, 110, 90,  overlayAlpha);
        g2.setColor(headCol);
        g2.drawString(headline, hx, hy);

        // Subtitle
        String winner = won ? battle.getPlayer().getName() : battle.getEnemy().getName();
        String loser  = won ? battle.getEnemy().getName()  : battle.getPlayer().getName();
        String sub = winner + " defeated " + loser + "!";
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        FontMetrics fmS = g2.getFontMetrics();
        g2.setColor(new Color(210, 210, 210, overlayAlpha));
        g2.drawString(sub, bx + (bw - fmS.stringWidth(sub)) / 2, hy + fmH.getHeight() - 8);
    }
}