package Combat.CharacterBattle;

import Entities.Character;
import Entities.Entity;
import Entities.PassiveHandler.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * CharacterBattlePanel — full-screen battle view for Character vs Character.
 *
 * Mirrors BattlePanel exactly in structure and visual style.
 * Both fighters are Characters — both run their full active passive kits.
 * Battle starts automatically after "Ready…" / "Fight!!" intro.
 */
public class CharacterBattlePanel extends JPanel implements CharacterBattle.BattleListener {

    // ── Window ────────────────────────────────────────────────────────────────
    private static final int W = 1280;
    private static final int H = 720;

    // ── Fighter geometry ──────────────────────────────────────────────────────
    private static final double ISO_SCALE     = 0.55;
    private static final int    GROUND_BASE   = 610;
    private static final int    SPRITE_W      = 72;
    private static final int    SPRITE_H      = 90;
    private static final int    ENGAGE_DIST   = 110;
    private static final int    APPROACH_SPEED = 5;

    private static final int F1_START_X   = -500;
    private static final int F2_START_X   =  500;
    private static final int F1_TARGET_X  = -ENGAGE_DIST / 2;
    private static final int F2_TARGET_X  =  ENGAGE_DIST / 2;
    private static final int F1_WORLD_Y   = 60;
    private static final int F2_WORLD_Y   = 90;

    // ── HUD ───────────────────────────────────────────────────────────────────
    private static final int HUD_H    = 70;
    private static final int BAR_W    = 320;
    private static final int BAR_H    = 18;
    private static final int BAR_MARGIN = 30;

    // ── Timing ────────────────────────────────────────────────────────────────
    private static final int TICK_MS       = 16;
    private static final int READY_MS      = 1200;
    private static final int FIGHT_MS      = 800;
    private static final int HIT_FLASH_MS  = 100;
    private static final int BOB_AMPLITUDE = 3;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final Color BG_TOP      = new Color(60, 80, 50);
    private static final Color BG_BOT      = new Color(100, 120, 70);
    private static final Color GROUND_NEAR = new Color(130, 110, 70);
    private static final Color GROUND_FAR  = new Color(80, 90, 55);
    // Fighter 1 — blue, Fighter 2 — teal (both Characters, visually distinct)
    private static final Color F1_COL      = new Color(50, 120, 240);
    private static final Color F1_FLASH    = new Color(180, 220, 255);
    private static final Color F2_COL      = new Color(20, 170, 150);
    private static final Color F2_FLASH    = new Color(130, 240, 220);
    private static final Color HUD_BG      = new Color(0, 0, 0, 160);
    private static final Color BAR_GREEN   = new Color(60, 200, 60);
    private static final Color BAR_YELLOW  = new Color(230, 190, 0);
    private static final Color BAR_RED     = new Color(210, 40, 40);
    private static final Color BAR_EMPTY   = new Color(40, 40, 40);

    // ── Battle engine ─────────────────────────────────────────────────────────
    private final CharacterBattle battle;

    // ── Render / master timer ─────────────────────────────────────────────────
    private final javax.swing.Timer renderTimer;

    // ── Intro sequence ────────────────────────────────────────────────────────
    private enum IntroState { READY, FIGHT, DONE }
    private IntroState introState  = IntroState.READY;
    private int        introAlpha  = 0;
    private int        introTick   = 0;
    private static final int INTRO_FADE_TICKS = 12;
    private static final int READY_TICKS = (int)(READY_MS / (double) TICK_MS);
    private static final int FIGHT_TICKS = (int)(FIGHT_MS / (double) TICK_MS);

    // ── Attack timers ─────────────────────────────────────────────────────────
    private javax.swing.Timer f1AttackTimer;
    private javax.swing.Timer f2AttackTimer;

    // ── World positions ───────────────────────────────────────────────────────
    private double  f1WorldX     = F1_START_X;
    private double  f2WorldX     = F2_START_X;
    private boolean approaching  = false;   // armed by beginCombat()
    private boolean combatStarted = false;
    private int     bobTick      = 0;

    // ── Flash state ───────────────────────────────────────────────────────────
    private boolean f1Flashing = false;
    private boolean f2Flashing = false;
    private int     f1FlashTick = 0;
    private int     f2FlashTick = 0;

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
    private CharacterBattle.BattleState battleResult = null;
    private int overlayAlpha = 0;
    private javax.swing.Timer overlayFadeTimer;

    // ── Placeholder sprites ───────────────────────────────────────────────────
    private final BufferedImage f1Sprite;
    private final BufferedImage f2Sprite;
    private final BufferedImage bgImage;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────────────

    public CharacterBattlePanel(CharacterBattle battle) {
        this.battle = battle;
        battle.addListener(this);

        setPreferredSize(new Dimension(W, H));
        setLayout(null);

        f1Sprite = makePlaceholderSprite(F1_COL, battle.getFighter1().getName().substring(0, 1));
        f2Sprite = makePlaceholderSprite(F2_COL, battle.getFighter2().getName().substring(0, 1));

        BufferedImage loadedBg;
        try {
            String path = "/spar_bg.png";
            loadedBg = javax.imageio.ImageIO.read(getClass().getResource(path));
        } catch (Exception e) {
            loadedBg = makePlaceholderBg();
        }
        bgImage = loadedBg;

        renderTimer = new javax.swing.Timer(TICK_MS, e -> tick());
        renderTimer.start();
    }

    // ── Master tick ───────────────────────────────────────────────────────────

    private void tick() {
        tickIntro();
        if (approaching) tickApproach();
        if (!approaching && combatStarted) bobTick++;

        if (f1Flashing && ++f1FlashTick > HIT_FLASH_MS / TICK_MS) { f1Flashing = false; f1FlashTick = 0; }
        if (f2Flashing && ++f2FlashTick > HIT_FLASH_MS / TICK_MS) { f2Flashing = false; f2FlashTick = 0; }

        floatingTexts.removeIf(ft -> !ft.tick());
        repaint();
    }

    // ── Intro sequence ────────────────────────────────────────────────────────

    private void tickIntro() {
        if (introState == IntroState.DONE) return;
        introTick++;

        if (introState == IntroState.READY) {
            introAlpha = Math.min(255, introTick * (255 / INTRO_FADE_TICKS));
            if (introTick >= READY_TICKS) { introState = IntroState.FIGHT; introTick = 0; introAlpha = 0; }
        } else if (introState == IntroState.FIGHT) {
            introAlpha = Math.min(255, introTick * (255 / INTRO_FADE_TICKS));
            if (introTick >= FIGHT_TICKS) { introState = IntroState.DONE; introAlpha = 0; beginCombat(); }
        }
    }

    private void beginCombat() {
        battle.start();
        combatStarted = true;
        approaching   = true;
    }

    // ── Approach ──────────────────────────────────────────────────────────────

    private void tickApproach() {
        if (!combatStarted) return;
        boolean d1 = false, d2 = false;
        if (f1WorldX < F1_TARGET_X) f1WorldX = Math.min(f1WorldX + APPROACH_SPEED, F1_TARGET_X); else d1 = true;
        if (f2WorldX > F2_TARGET_X) f2WorldX = Math.max(f2WorldX - APPROACH_SPEED, F2_TARGET_X); else d2 = true;

        if (d1 && d2) {
            approaching = false;
            battle.setEngaged();
            startCombatTimers();
        }
    }

    private void startCombatTimers() {
        f1AttackTimer = new javax.swing.Timer(battle.getFighter1().getAttackSpeed(), e -> battle.fighter1Tick());
        f1AttackTimer.setInitialDelay(0);
        f1AttackTimer.start();

        f2AttackTimer = new javax.swing.Timer(battle.getFighter2().getAttackSpeed(), e -> battle.fighter2Tick());
        f2AttackTimer.setInitialDelay(0);
        f2AttackTimer.start();
    }

    private void stopAllTimers() {
        renderTimer.stop();
        if (f1AttackTimer    != null) f1AttackTimer.stop();
        if (f2AttackTimer    != null) f2AttackTimer.stop();
        if (overlayFadeTimer != null) overlayFadeTimer.stop();
        battle.stop();
    }

    // ── CharacterBattle.BattleListener ───────────────────────────────────────

    @Override
    public void onFighter1Attack(String log, int damage, boolean isCrit, boolean isMiss) {
        SwingUtilities.invokeLater(() -> {
            if (isMiss) spawnMissPopup(true);  // miss on f1 means f2 attacked and missed
            else { f2Flashing = true; f2FlashTick = 0; showDmgPopup(false, damage, isCrit); }
        });
    }

    @Override
    public void onFighter2Attack(String log, int damage, boolean isCrit, boolean isMiss) {
        SwingUtilities.invokeLater(() -> {
            if (isMiss) spawnMissPopup(true);
            else { f1Flashing = true; f1FlashTick = 0; showDmgPopup(true, damage, isCrit); }
        });
    }

    @Override
    public void onPassive(String log, Entity owner, int amount, boolean isHeal) {
        SwingUtilities.invokeLater(() -> {
            boolean onF1 = isHeal
                    ? (owner == battle.getFighter1())
                    : (owner != battle.getFighter1());
            spawnPassivePopup(onF1, amount, isHeal);
        });
    }

    @Override
    public void onPassiveMiss(String log, Entity owner, Entity target, String passiveName) {
        SwingUtilities.invokeLater(() -> {
            // Popup appears on the target (the one who avoided the hit)
            boolean onF1 = (owner != battle.getFighter1());
            spawnMissPopup(onF1);
        });
    }

    @Override
    public void onBattleEnd(CharacterBattle.BattleState result) {
        SwingUtilities.invokeLater(() -> {
            if (f1AttackTimer != null) f1AttackTimer.stop();
            if (f2AttackTimer != null) f2AttackTimer.stop();
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

    private void showDmgPopup(boolean onF1, int dmg, boolean isCrit) {
        Point sp = spriteScreenPos(onF1 ? f1WorldX : f2WorldX,
                onF1 ? F1_WORLD_Y : F2_WORLD_Y);
        String txt = "-" + dmg + (isCrit ? "!" : "");
        floatingTexts.add(new FloatingText(txt, sp.x + SPRITE_W / 2f, sp.y - 10,
                isCrit, false, false, false, false));
    }

    private void spawnMissPopup(boolean onF1) {
        Point sp = spriteScreenPos(onF1 ? f1WorldX : f2WorldX,
                onF1 ? F1_WORLD_Y : F2_WORLD_Y);
        floatingTexts.add(new FloatingText("MISS!", sp.x + SPRITE_W / 2f, sp.y - 10,
                false, false, false, true, false));
    }

    private void spawnPassivePopup(boolean onF1, int amount, boolean isHeal) {
        Point sp = spriteScreenPos(onF1 ? f1WorldX : f2WorldX,
                onF1 ? F1_WORLD_Y : F2_WORLD_Y);
        String txt = (isHeal ? "+" : "-") + amount;
        boolean leftAnchored;
        float sx, sy;
        if (isHeal) {
            leftAnchored = !onF1;
            sx = onF1 ? sp.x - 8 : sp.x + SPRITE_W + 8;
            sy = sp.y + SPRITE_H / 2f;
        } else {
            leftAnchored = false;
            sx = sp.x + SPRITE_W / 2f;
            sy = sp.y - 10;
        }
        floatingTexts.add(new FloatingText(txt, sx, sy, false, isHeal, !isHeal, false, leftAnchored));
    }

    // ── Projection ────────────────────────────────────────────────────────────

    private Point spriteScreenPos(double worldX, double worldY) {
        int sx = (int)(W / 2.0 + worldX) - SPRITE_W / 2;
        int sy = (int)(GROUND_BASE - worldY * ISO_SCALE) - SPRITE_H;
        return new Point(sx, sy);
    }

    // ── Placeholder generators ────────────────────────────────────────────────

    private BufferedImage makePlaceholderSprite(Color base, String initial) {
        BufferedImage img = new BufferedImage(SPRITE_W, SPRITE_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int[] xs = { 4, SPRITE_W - 4, SPRITE_W - 10, 10 };
        int[] ys = { 0, 0, SPRITE_H, SPRITE_H };
        g.setColor(base);
        g.fillPolygon(xs, ys, 4);
        g.setColor(base.darker());
        g.setStroke(new BasicStroke(2));
        g.drawPolygon(xs, ys, 4);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(255, 255, 255, 200));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(initial, (SPRITE_W - fm.stringWidth(initial)) / 2, SPRITE_H / 2 + fm.getAscent() / 2 - 4);
        g.dispose();
        return img;
    }

    private BufferedImage makePlaceholderBg() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        GradientPaint sky = new GradientPaint(0, 0, new Color(100, 130, 180),
                0, GROUND_BASE - 60, new Color(160, 185, 140));
        g.setPaint(sky);
        g.fillRect(0, 0, W, GROUND_BASE - 60);
        int[] gx = { 0, W, W, 0 };
        int[] gy = { GROUND_BASE - 60, GROUND_BASE - 60, H, H };
        GradientPaint ground = new GradientPaint(0, GROUND_BASE - 60, GROUND_FAR,
                0, H,               GROUND_NEAR);
        g.setPaint(ground);
        g.fillPolygon(gx, gy, 4);
        g.setColor(new Color(0, 0, 0, 25));
        g.setStroke(new BasicStroke(1));
        int vp = W / 2;
        for (int i = -8; i <= 8; i++) g.drawLine(vp, GROUND_BASE - 60, vp + i * 90, H);
        for (int row = 0; row <= 8; row++) {
            double t = row / 8.0;
            int y = (int)(GROUND_BASE - 60 + t * (H - (GROUND_BASE - 60)));
            g.drawLine((int)(vp - vp * t), y, (int)(vp + (W - vp) * t), y);
        }
        g.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, 14));
        g.setColor(new Color(255, 255, 255, 80));
        String label = "[ Stage Background Placeholder — replace with /res/stage_bg.gif ]";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, (W - fm.stringWidth(label)) / 2, GROUND_BASE - 70);
        g.dispose();
        return img;
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
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        g2.drawImage(bgImage, 0, 0, null);

        drawSprite(g2, battle.getFighter1(), f1WorldX, F1_WORLD_Y, f1Sprite, f1Flashing ? F1_FLASH : null, false);
        drawSprite(g2, battle.getFighter2(), f2WorldX, F2_WORLD_Y, f2Sprite, f2Flashing ? F2_FLASH : null, true);

        drawHud(g2);

        for (FloatingText ft : floatingTexts) drawFloatingText(g2, ft);

        if (introState != IntroState.DONE) drawIntroText(g2);
        if (battleResult != null)          drawResultOverlay(g2);
    }

    // ── Draw: sprite ──────────────────────────────────────────────────────────

    private void drawSprite(Graphics2D g2, Entity entity, double worldX, double worldY,
                            BufferedImage sprite, Color flashColor, boolean flipX) {
        Point pos = spriteScreenPos(worldX, worldY);
        int sx = pos.x;
        int sy = pos.y + (!approaching && entity.isAlive()
                ? (int)(Math.sin(bobTick * 0.15 + (flipX ? Math.PI : 0)) * BOB_AMPLITUDE) : 0);

        int shadowY = (int)(GROUND_BASE - worldY * ISO_SCALE);
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillOval(sx + 8, shadowY - 8, SPRITE_W - 16, 14);

        if (flipX) g2.drawImage(sprite, sx + SPRITE_W, sy, -SPRITE_W, SPRITE_H, null);
        else       g2.drawImage(sprite, sx, sy, null);

        if (flashColor != null) {
            g2.setColor(new Color(flashColor.getRed(), flashColor.getGreen(), flashColor.getBlue(), 140));
            g2.fillRect(sx, sy, SPRITE_W, SPRITE_H);
        }

        if (!entity.isAlive()) {
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int pad = 14;
            g2.drawLine(sx + pad, sy + pad, sx + SPRITE_W - pad, sy + SPRITE_H - pad);
            g2.drawLine(sx + SPRITE_W - pad, sy + pad, sx + pad, sy + SPRITE_H - pad);
            g2.setStroke(new BasicStroke(1));
        }

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
        g2.setColor(HUD_BG);
        g2.fillRect(0, 0, W, HUD_H);

        Character f1 = battle.getFighter1();
        Character f2 = battle.getFighter2();
        drawHudEntry(g2, f1.getName(), f1, BAR_MARGIN, false);
        drawHudEntry(g2, f2.getName(), f2, W - BAR_MARGIN - BAR_W, true);
    }

    private void drawHudEntry(Graphics2D g2, String label, Entity entity, int barX, boolean rightAlign) {
        int barY = 16;
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int nx = rightAlign ? barX + BAR_W - fm.stringWidth(label) : barX;
        g2.setColor(Color.WHITE);
        g2.drawString(label, nx, barY + 11);

        int by = barY + 18;
        g2.setColor(BAR_EMPTY);
        g2.fillRoundRect(barX, by, BAR_W, BAR_H, BAR_H, BAR_H);

        double pct = entity.getHpPercent();
        int fillW = Math.max(0, (int)(BAR_W * pct));
        Color barCol = pct > 0.5 ? BAR_GREEN : pct > 0.25 ? BAR_YELLOW : BAR_RED;
        if (fillW > 0) {
            g2.setColor(barCol);
            g2.fillRoundRect(barX, by, fillW, BAR_H, BAR_H, BAR_H);
            g2.setColor(new Color(255, 255, 255, 50));
            g2.fillRoundRect(barX, by, fillW, BAR_H / 2, BAR_H, BAR_H);
        }
        g2.setColor(new Color(0, 0, 0, 120));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(barX, by, BAR_W, BAR_H, BAR_H, BAR_H);
        g2.setStroke(new BasicStroke(1));

        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        fm = g2.getFontMetrics();
        String hp = entity.getCurrentHp() + " / " + entity.getMaxHp();
        int hx = rightAlign ? barX + BAR_W - fm.stringWidth(hp) : barX;
        g2.setColor(new Color(220, 220, 220));
        g2.drawString(hp, hx, by + BAR_H + 13);

        if (entity instanceof Shielded s && s.getShieldHp() > 0) {
            double shieldPct = Math.min(1.0, (double) s.getShieldHp() / entity.getMaxHp());
            int shieldW = Math.max(4, (int)(BAR_W * shieldPct));
            g2.setColor(new Color(80, 130, 255, 150));
            g2.fillRoundRect(barX, by, shieldW, BAR_H, BAR_H, BAR_H);
            g2.setColor(new Color(160, 190, 255, 60));
            g2.fillRoundRect(barX, by, shieldW, BAR_H / 2, BAR_H, BAR_H);
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            fm = g2.getFontMetrics();
            String shieldStr = "Shield: " + s.getShieldHp();
            int shx = rightAlign ? barX + BAR_W - fm.stringWidth(shieldStr) : barX;
            g2.setColor(new Color(130, 170, 255));
            g2.drawString(shieldStr, shx, by + BAR_H + 26);
        }
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
        g2.setColor(new Color(0, 0, 0, introAlpha / 2));
        g2.drawString(text, tx + 3, ty + 3);
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), introAlpha));
        g2.drawString(text, tx, ty);
    }

    // ── Draw: floating text ───────────────────────────────────────────────────

    private void drawFloatingText(Graphics2D g2, FloatingText ft) {
        if (ft.isMiss) {
            g2.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, 15));
        } else {
            g2.setFont(new Font("SansSerif", Font.BOLD, ft.isCrit ? 20 : 15));
        }
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(ft.text);
        int dx = ft.leftAnchored ? (int) ft.x : (int) ft.x - tw / 2;
        g2.setColor(new Color(0, 0, 0, ft.alpha / 3));
        g2.drawString(ft.text, dx + 1, (int) ft.y + 1);
        Color c = ft.isMiss       ? new Color(200, 200, 200, ft.alpha)
                : ft.isHeal       ? new Color(80,  230, 80,  ft.alpha)
                : ft.isPassiveDmg ? new Color(80,  150, 255, ft.alpha)
                : ft.isCrit       ? new Color(255, 215, 0,   ft.alpha)
                :                   new Color(255, 80,  80,  ft.alpha);
        g2.setColor(c);
        g2.drawString(ft.text, dx, (int) ft.y);
    }

    // ── Draw: result overlay ──────────────────────────────────────────────────

    private void drawResultOverlay(Graphics2D g2) {
        boolean f1Won = battleResult == CharacterBattle.BattleState.FIGHTER1_WIN;
        g2.setColor(new Color(0, 0, 0, overlayAlpha / 2));
        g2.fillRect(0, 0, W, H);

        int bw = 500, bh = 160, bx = (W - bw) / 2, by = H / 2 - bh / 2;
        Color bannerCol = f1Won ? new Color(20, 60, 160, overlayAlpha)
                : new Color(10, 110, 100, overlayAlpha);
        g2.setColor(bannerCol);
        g2.fillRoundRect(bx, by, bw, bh, 20, 20);
        g2.setColor(new Color(255, 255, 255, Math.min(overlayAlpha + 30, 255)));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(bx, by, bw, bh, 20, 20);
        g2.setStroke(new BasicStroke(1));

        String headline = "VICTORY";
        g2.setFont(new Font("SansSerif", Font.BOLD, 48));
        FontMetrics fmH = g2.getFontMetrics();
        int hx = bx + (bw - fmH.stringWidth(headline)) / 2;
        int hy = by + bh / 2 + 4;
        g2.setColor(new Color(0, 0, 0, overlayAlpha));
        g2.drawString(headline, hx + 3, hy + 3);
        g2.setColor(new Color(150, 200, 255, overlayAlpha));
        g2.drawString(headline, hx, hy);

        String winner = f1Won ? battle.getFighter1().getName() : battle.getFighter2().getName();
        String loser  = f1Won ? battle.getFighter2().getName() : battle.getFighter1().getName();
        String sub    = winner + " defeated " + loser + "!";
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        FontMetrics fmS = g2.getFontMetrics();
        g2.setColor(new Color(210, 210, 210, overlayAlpha));
        g2.drawString(sub, bx + (bw - fmS.stringWidth(sub)) / 2, hy + fmH.getHeight() - 8);
    }
}