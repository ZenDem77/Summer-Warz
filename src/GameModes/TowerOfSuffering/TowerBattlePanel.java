package GameModes.TowerOfSuffering;

import Combat.NormalBattle.Battle;
import Entities.Character;
import Entities.Enemy;
import Entities.Entity;
import Entities.PassiveHandler.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * TowerBattlePanel — battle screen for Tower of Suffering floor mode.
 *
 * Additions over standard BattlePanel:
 *  - Floor label in HUD ("Floor 5" / "★ BOSS — Floor 10")
 *  - [i] info button (below floor label, top-centre) pauses combat and opens
 *    an enemy info overlay with left/right navigation between all enemies
 *  - Victory overlay shows "Exit" and "Next Floor" buttons — no auto-advance
 *
 * All standard mechanics preserved: damage tracking, overkill prevention,
 * victory stats with percentages, floating text, passive system, etc.
 *
 * Package: GameModes.TowerOfSuffering
 * Extra imports needed:
 *   Combat.NormalBattle.Battle
 *   Entities.Character, Enemy, Entity, Passive
 */
public class TowerBattlePanel extends JPanel implements Battle.BattleListener {

    // ── Window ────────────────────────────────────────────────────────────────
    private static final int W = 1280;
    private static final int H = 720;

    // ── Fighter geometry ──────────────────────────────────────────────────────
    private static final double ISO_SCALE      = 0.55;
    private static final int    GROUND_BASE    = 530;
    private static final int    SPRITE_W       = 72;
    private static final int    SPRITE_H       = 90;
    private static final int    ENGAGE_DIST    = 110;
    private static final int    APPROACH_SPEED = 5;

    private static final int PLAYER_START_X  = -500;
    private static final int ENEMY_START_X   =  500;
    private static final int PLAYER_TARGET_X = -ENGAGE_DIST / 2;
    private static final int ENEMY_TARGET_X  =  ENGAGE_DIST / 2;
    private static final int PLAYER_WORLD_Y  = 60;
    private static final int ENEMY_WORLD_Y   = 90;

    // ── HUD ───────────────────────────────────────────────────────────────────
    private static final int HUD_H      = 90;
    private static final int BAR_W      = 320;
    private static final int BAR_H      = 18;
    private static final int BAR_MARGIN = 30;
    private static final int ICON_SIZE  = 20;
    private static final int ICON_GAP   = 6;

    // ── Timing ────────────────────────────────────────────────────────────────
    private static final int TICK_MS             = 16;
    private static final int READY_MS            = 1200;
    private static final int FIGHT_MS            = 800;
    private static final int HIT_FLASH_MS        = 100;
    private static final int BOB_AMPLITUDE       = 3;
    private static final int NEXT_ENTER_DELAY_MS = 800;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final Color GROUND_NEAR  = new Color(130, 110, 70);
    private static final Color GROUND_FAR   = new Color(80,  90,  55);
    private static final Color PLAYER_COL   = new Color(50,  120, 240);
    private static final Color PLAYER_FLASH = new Color(180, 220, 255);
    private static final Color ENEMY_COL    = new Color(220, 50,  50);
    private static final Color ENEMY_FLASH  = new Color(255, 180, 160);
    private static final Color HUD_BG       = new Color(0,   0,   0,   170);
    private static final Color BAR_GREEN    = new Color(60,  200, 60);
    private static final Color BAR_YELLOW   = new Color(230, 190, 0);
    private static final Color BAR_RED      = new Color(210, 40,  40);
    private static final Color BAR_EMPTY    = new Color(40,  40,  40);
    private static final Color BOSS_GOLD    = new Color(255, 200, 50);

    // ── Core references ───────────────────────────────────────────────────────
    private final Battle battle;
    private final Floor  floor;

    // ── Tower callback interface ──────────────────────────────────────────────

    public interface TowerBattleListener {
        void onNextFloor();
        void onExit();
    }

    private final TowerBattleListener towerListener;

    // ── Timers ────────────────────────────────────────────────────────────────
    private final javax.swing.Timer renderTimer;
    private javax.swing.Timer playerAttackTimer;
    private javax.swing.Timer enemyAttackTimer;
    private javax.swing.Timer overlayFadeTimer;

    // ── Intro sequence ────────────────────────────────────────────────────────
    private enum IntroState { READY, FIGHT, DONE }
    private IntroState introState = IntroState.READY;
    private int introAlpha = 0, introTick = 0;
    private static final int INTRO_FADE_TICKS = 12;
    private static final int READY_TICKS = READY_MS / TICK_MS;
    private static final int FIGHT_TICKS = FIGHT_MS / TICK_MS;

    // ── World positions & animation ───────────────────────────────────────────
    private double  playerWorldX   = PLAYER_START_X;
    private double  enemyWorldX    = ENEMY_START_X;
    private boolean approaching    = false;
    private boolean combatStarted  = false;
    private boolean waitingForNext = false;
    private int     bobTick        = 0;

    // ── Flash ─────────────────────────────────────────────────────────────────
    private boolean playerFlashing = false, enemyFlashing = false;
    private int     playerFlashTick = 0,    enemyFlashTick = 0;

    // ── Floating text ─────────────────────────────────────────────────────────
    private final java.util.List<FloatingText> floatingTexts = new java.util.ArrayList<>();

    private static class FloatingText {
        static final int DURATION_MS = 700, RISE_PX = 40;
        String text; float x, y; int alpha = 255;
        boolean isCrit, isHeal, isPassiveDmg, isMiss, leftAnchored;
        int ticksLeft; float dy; int dAlpha;
        FloatingText(String text, float x, float y,
                     boolean isCrit, boolean isHeal, boolean isPassiveDmg,
                     boolean isMiss, boolean leftAnchored) {
            this.text = text; this.x = x; this.y = y;
            this.isCrit = isCrit; this.isHeal = isHeal;
            this.isPassiveDmg = isPassiveDmg; this.isMiss = isMiss;
            this.leftAnchored = leftAnchored;
            int total = DURATION_MS / TICK_MS;
            ticksLeft = total; dy = (float) RISE_PX / total; dAlpha = 255 / total;
        }
        boolean tick() { y -= dy; alpha -= dAlpha; ticksLeft--; return alpha > 0 && ticksLeft > 0; }
    }

    // ── Result overlay ────────────────────────────────────────────────────────
    private Battle.BattleState battleResult = null;
    private int overlayAlpha = 0;

    // ── Info overlay ──────────────────────────────────────────────────────────
    private boolean infoPanelOpen  = false;
    private int     infoEnemyIndex = 0;

    // ── Clickable areas (updated each paint, read by mouse listener) ──────────
    private final Rectangle infoBtnArea   = new Rectangle();
    private final Rectangle infoCloseArea = new Rectangle();
    private final Rectangle infoLeftArea  = new Rectangle();
    private final Rectangle infoRightArea = new Rectangle();
    private final Rectangle btnNextFloor  = new Rectangle();
    private final Rectangle btnExit       = new Rectangle();

    // Hover tracking
    private boolean hoverInfo = false, hoverClose = false;
    private boolean hoverLeft = false, hoverRight = false;
    private boolean hoverNext = false, hoverExitBtn = false;

    // ── Sprite cache ──────────────────────────────────────────────────────────
    private final Map<Entity, BufferedImage> spriteCache = new HashMap<>();
    private final BufferedImage bgImage;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────────────

    public TowerBattlePanel(Battle battle, Floor floor, TowerBattleListener listener) {
        this.battle        = battle;
        this.floor         = floor;
        this.towerListener = listener;
        battle.addListener(this);

        setPreferredSize(new Dimension(W, H));
        setLayout(null);

        bgImage = makePlaceholderBg();
        battle.getPlayerTeam().forEach(c -> spriteCache.put(c, makePlaceholderSprite(PLAYER_COL, c.getName().substring(0, 1))));
        battle.getEnemyTeam() .forEach(e -> spriteCache.put(e, makePlaceholderSprite(ENEMY_COL,  e.getName().substring(0, 1))));

        wireMouseInput();

        renderTimer = new javax.swing.Timer(TICK_MS, e -> tick());
        renderTimer.start();
    }

    // ── Input wiring ──────────────────────────────────────────────────────────

    private void wireMouseInput() {
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                hoverInfo    = infoBtnArea.contains(p);
                hoverClose   = infoCloseArea.contains(p);
                hoverLeft    = infoLeftArea.contains(p);
                hoverRight   = infoRightArea.contains(p);
                hoverNext    = btnNextFloor.contains(p);
                hoverExitBtn = btnExit.contains(p);
                repaint();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Point p = e.getPoint();

                // [i] button — open info panel (only during active combat)
                if (infoBtnArea.contains(p) && battleResult == null && !infoPanelOpen) {
                    openInfoPanel(); return;
                }

                // Info overlay controls
                if (infoPanelOpen) {
                    if (infoCloseArea.contains(p)) { closeInfoPanel(); return; }
                    if (infoLeftArea.contains(p) && infoEnemyIndex > 0) {
                        infoEnemyIndex--; repaint(); return;
                    }
                    if (infoRightArea.contains(p) && infoEnemyIndex < battle.getEnemyTeam().size() - 1) {
                        infoEnemyIndex++; repaint(); return;
                    }
                    return; // eat all other clicks while info is open
                }

                // Victory buttons (only active after overlay fades in)
                if (battleResult == Battle.BattleState.PLAYER_WIN && overlayAlpha >= 180) {
                    if (btnNextFloor.contains(p)) { towerListener.onNextFloor(); return; }
                    if (btnExit.contains(p))      { towerListener.onExit();      return; }
                }
            }
        });
    }

    // ── Info panel open / close ───────────────────────────────────────────────

    private void openInfoPanel() {
        infoPanelOpen  = true;
        infoEnemyIndex = battle.getEnemyIndex();  // default to current active enemy
        pauseCombat();
        repaint();
    }

    private void closeInfoPanel() {
        infoPanelOpen = false;
        resumeCombat();
        repaint();
    }

    private void pauseCombat() {
        if (playerAttackTimer != null) playerAttackTimer.stop();
        if (enemyAttackTimer  != null) enemyAttackTimer.stop();
        battle.pause();
    }

    private void resumeCombat() {
        if (battle.getState() != Battle.BattleState.ONGOING) return;
        if (playerAttackTimer != null) playerAttackTimer.start();
        if (enemyAttackTimer  != null) enemyAttackTimer.start();
        battle.resume();
    }

    // ── Master tick ───────────────────────────────────────────────────────────

    private void tick() {
        if (!infoPanelOpen) {
            tickIntro();
            if (approaching) tickApproach();
            if (!approaching && combatStarted && !waitingForNext) bobTick++;
        }
        if (playerFlashing && ++playerFlashTick > HIT_FLASH_MS / TICK_MS) { playerFlashing = false; playerFlashTick = 0; }
        if (enemyFlashing  && ++enemyFlashTick > HIT_FLASH_MS / TICK_MS)  { enemyFlashing  = false; enemyFlashTick  = 0; }
        floatingTexts.removeIf(ft -> !ft.tick());
        repaint();
    }

    // ── Intro ─────────────────────────────────────────────────────────────────

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
        playerWorldX  = PLAYER_START_X;
        enemyWorldX   = ENEMY_START_X;
    }

    // ── Approach ──────────────────────────────────────────────────────────────

    private void tickApproach() {
        if (!combatStarted) return;
        boolean pd = false, ed = false;
        if (playerWorldX < PLAYER_TARGET_X) playerWorldX = Math.min(playerWorldX + APPROACH_SPEED, PLAYER_TARGET_X); else pd = true;
        if (enemyWorldX  > ENEMY_TARGET_X)  enemyWorldX  = Math.max(enemyWorldX  - APPROACH_SPEED, ENEMY_TARGET_X);  else ed = true;
        if (pd && ed) {
            approaching = false;
            if (waitingForNext) {
                waitingForNext = false;
                battle.setNextEngaged();
                restartCombatTimers();
            } else {
                battle.setEngaged();
                startCombatTimers();
            }
        }
    }

    // ── Combat timers ─────────────────────────────────────────────────────────

    private void startCombatTimers() {
        playerAttackTimer = new javax.swing.Timer(battle.getActivePlayer().getAttackSpeed(), e -> battle.playerTick());
        playerAttackTimer.setInitialDelay(0); playerAttackTimer.start();
        enemyAttackTimer  = new javax.swing.Timer(battle.getActiveEnemy().getAttackSpeed(),  e -> battle.enemyTick());
        enemyAttackTimer.setInitialDelay(0);  enemyAttackTimer.start();
    }

    private void stopCombatTimers() {
        if (playerAttackTimer != null) playerAttackTimer.stop();
        if (enemyAttackTimer  != null) enemyAttackTimer.stop();
    }

    private void restartCombatTimers() { stopCombatTimers(); startCombatTimers(); }

    private void stopAllTimers() {
        renderTimer.stop(); stopCombatTimers();
        if (overlayFadeTimer != null) overlayFadeTimer.stop();
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
            boolean onPlayer = isHeal ? (owner == battle.getActivePlayer())
                    : (owner != battle.getActivePlayer());
            spawnPassivePopup(onPlayer, amount, isHeal);
        });
    }

    @Override
    public void onFighterEnter(boolean isPlayer, Entity fighter, int remaining) {
        SwingUtilities.invokeLater(() -> {
            stopCombatTimers();
            waitingForNext = true;
            new javax.swing.Timer(NEXT_ENTER_DELAY_MS, e -> {
                ((javax.swing.Timer) e.getSource()).stop();
                if (isPlayer) playerWorldX = PLAYER_START_X;
                else          enemyWorldX  = ENEMY_START_X;
                approaching = true;
            }) {{ setRepeats(false); start(); }};
        });
    }

    @Override
    public void onBattleEnd(Battle.BattleState result) {
        SwingUtilities.invokeLater(() -> {
            stopCombatTimers();
            if (infoPanelOpen) closeInfoPanel();
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

    private Point spritePos(double worldX, double worldY) {
        return new Point((int)(W / 2.0 + worldX) - SPRITE_W / 2,
                (int)(GROUND_BASE - worldY * ISO_SCALE) - SPRITE_H);
    }

    private void showDmgPopup(boolean onPlayer, int dmg, boolean isCrit) {
        Point sp = spritePos(onPlayer ? playerWorldX : enemyWorldX, onPlayer ? PLAYER_WORLD_Y : ENEMY_WORLD_Y);
        if (dmg <= 0) {
            // Shield absorbed the entire hit — show BLOCK in blue
            floatingTexts.add(new TowerBattlePanel.FloatingText("BLOCK", sp.x + SPRITE_W / 2f, sp.y - 10,
                    false, false, true, false, false));
            return;
        }
        String txt = "-" + dmg + (isCrit ? "!" : "");
        floatingTexts.add(new TowerBattlePanel.FloatingText(txt, sp.x + SPRITE_W / 2f, sp.y - 10,
                isCrit, false, false, false, false));
    }

    private void spawnMissPopup(boolean onPlayer) {
        Point sp = spritePos(onPlayer ? playerWorldX : enemyWorldX, onPlayer ? PLAYER_WORLD_Y : ENEMY_WORLD_Y);
        floatingTexts.add(new FloatingText("MISS!", sp.x + SPRITE_W / 2f, sp.y - 10,
                false, false, false, true, false));
    }

    private void spawnPassivePopup(boolean onPlayer, int amount, boolean isHeal) {
        Point sp = spritePos(onPlayer ? playerWorldX : enemyWorldX, onPlayer ? PLAYER_WORLD_Y : ENEMY_WORLD_Y);
        String txt = (isHeal ? "+" : "-") + amount;
        boolean la; float sx, sy;
        if (isHeal) {
            la = !onPlayer;
            sx = onPlayer ? sp.x - 8 : sp.x + SPRITE_W + 8;
            sy = sp.y + SPRITE_H / 2f;
        } else { la = false; sx = sp.x + SPRITE_W / 2f; sy = sp.y - 10; }
        floatingTexts.add(new FloatingText(txt, sx, sy, false, isHeal, !isHeal, false, la));
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

        drawSprite(g2, battle.getActivePlayer(), playerWorldX, PLAYER_WORLD_Y,
                spriteCache.get(battle.getActivePlayer()), playerFlashing ? PLAYER_FLASH : null, false);
        drawSprite(g2, battle.getActiveEnemy(), enemyWorldX, ENEMY_WORLD_Y,
                spriteCache.get(battle.getActiveEnemy()), enemyFlashing ? ENEMY_FLASH : null, true);

        drawHud(g2);
        drawPlayerRoster(g2);
        drawEnemyCount(g2);

        for (FloatingText ft : floatingTexts) drawFloatingText(g2, ft);

        if (introState != IntroState.DONE)  drawIntroText(g2);
        if (infoPanelOpen)                  drawInfoOverlay(g2);
        if (battleResult != null)           drawResultOverlay(g2);
    }

    // ── Draw: sprite ──────────────────────────────────────────────────────────

    private void drawSprite(Graphics2D g2, Entity entity, double worldX, double worldY,
                            BufferedImage sprite, Color flashColor, boolean flipX) {
        Point pos = spritePos(worldX, worldY);
        int sx = pos.x;
        int sy = pos.y + (!approaching && entity.isAlive() && !waitingForNext
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

        drawHudEntry(g2, battle.getActivePlayer(), BAR_MARGIN, false);
        drawHudEntry(g2, battle.getActiveEnemy(),  W - BAR_MARGIN - BAR_W, true);

        // ── Floor label (top-centre) ──────────────────────────────────────────
        boolean boss = floor.isBoss();
        String floorLabel = boss ? "★  BOSS  —  Floor " + floor.getFloorNumber()
                : "Floor " + floor.getFloorNumber();
        g2.setFont(new Font("SansSerif", Font.BOLD, boss ? 16 : 14));
        FontMetrics flFm = g2.getFontMetrics();
        int flX = (W - flFm.stringWidth(floorLabel)) / 2;
        int flY = 18;
        g2.setColor(new Color(0, 0, 0, 140));
        g2.drawString(floorLabel, flX + 1, flY + 1);
        g2.setColor(boss ? BOSS_GOLD : Color.WHITE);
        g2.drawString(floorLabel, flX, flY);

        // ── [i] info button (below floor label) ───────────────────────────────
        int ibW = 30, ibH = 20;
        int ibX = (W - ibW) / 2;
        int ibY = flY + 8;
        infoBtnArea.setBounds(ibX, ibY, ibW, ibH);

        boolean canClick = battleResult == null && !infoPanelOpen;
        g2.setColor(hoverInfo && canClick ? new Color(80, 130, 200, 220) : new Color(30, 30, 60, 200));
        g2.fillRoundRect(ibX, ibY, ibW, ibH, 6, 6);
        g2.setColor(canClick ? new Color(150, 180, 255) : new Color(70, 70, 90));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(ibX, ibY, ibW, ibH, 6, 6);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, 12));
        FontMetrics iFm = g2.getFontMetrics();
        g2.setColor(Color.WHITE);
        g2.drawString("i", ibX + (ibW - iFm.stringWidth("i")) / 2, ibY + ibH - 4);
    }

    private void drawHudEntry(Graphics2D g2, Entity entity, int barX, boolean rightAlign) {
        int barY = 12;
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        String name = entity.getName();
        int nx = rightAlign ? barX + BAR_W - fm.stringWidth(name) : barX;
        g2.setColor(Color.WHITE);
        g2.drawString(name, nx, barY + 11);

        int by = barY + 16;
        g2.setColor(BAR_EMPTY);
        g2.fillRoundRect(barX, by, BAR_W, BAR_H, BAR_H, BAR_H);
        double pct = entity.getHpPercent();
        int fillW = Math.max(0, (int)(BAR_W * pct));
        Color col = pct > 0.5 ? BAR_GREEN : pct > 0.25 ? BAR_YELLOW : BAR_RED;
        if (fillW > 0) {
            g2.setColor(col);
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

        // ── Shield bar + text (only when shield is active) ──────────────────
        if (entity instanceof Shielded s && s.getShieldHp() > 0) {
            // Blue overlay on HP bar proportional to shield / maxHp
            double shieldPct = Math.min(1.0, (double) s.getShieldHp() / entity.getMaxHp());
            int shieldW = Math.max(4, (int)(BAR_W * shieldPct));
            g2.setColor(new Color(80, 130, 255, 150));
            g2.fillRoundRect(barX, by, shieldW, BAR_H, BAR_H, BAR_H);
            // Gloss on shield bar
            g2.setColor(new Color(160, 190, 255, 60));
            g2.fillRoundRect(barX, by, shieldW, BAR_H / 2, BAR_H, BAR_H);

            // Shield text below HP numbers
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            fm = g2.getFontMetrics();
            String shieldStr = "Shield: " + s.getShieldHp();
            int shx = rightAlign ? barX + BAR_W - fm.stringWidth(shieldStr) : barX;
            g2.setColor(new Color(130, 170, 255));
            g2.drawString(shieldStr, shx, by + BAR_H + 26);
        }
    }

    // ── Draw: bottom strip ────────────────────────────────────────────────────

    private void drawPlayerRoster(Graphics2D g2) {
        java.util.List<Character> team = battle.getPlayerTeam();
        int startX = 20, y = H - ICON_SIZE - 16;
        for (int i = 0; i < team.size(); i++) {
            Entity f = team.get(i);
            int x = startX + i * (ICON_SIZE + ICON_GAP);
            boolean active = i == battle.getPlayerIndex(), dead = !f.isAlive();
            g2.setColor(dead ? new Color(40,40,40,180) : active ? new Color(255,210,60,220)
                    : i < battle.getPlayerIndex() ? new Color(60,60,60,180) : new Color(80,100,160,200));
            g2.fillRoundRect(x, y, ICON_SIZE, ICON_SIZE, 4, 4);
            g2.setColor(active ? new Color(255,230,80) : new Color(80,80,80));
            g2.setStroke(new BasicStroke(active ? 2 : 1));
            g2.drawRoundRect(x, y, ICON_SIZE, ICON_SIZE, 4, 4);
            g2.setStroke(new BasicStroke(1));
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            FontMetrics fm = g2.getFontMetrics();
            String init = f.getName().substring(0, 1);
            g2.setColor(dead ? new Color(120,120,120) : Color.WHITE);
            g2.drawString(init, x + (ICON_SIZE - fm.stringWidth(init)) / 2, y + ICON_SIZE - 5);
        }
    }

    private void drawEnemyCount(Graphics2D g2) {
        int remaining = battle.getEnemyTeam().size() - battle.getEnemyIndex();
        String text = "Enemies remaining: " + remaining;
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text), pw = tw + 24, ph = 34;
        int px = W - pw - 20, py = H - ph - 16;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(px, py, pw, ph, 10, 10);
        int tx = px + (pw - tw) / 2;
        int ty = py + (ph + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(text, tx + 1, ty + 1);
        g2.setColor(remaining > 1 ? new Color(220, 100, 100) : new Color(255, 160, 60));
        g2.drawString(text, tx, ty);
    }

    // ── Draw: intro text ──────────────────────────────────────────────────────

    private void drawIntroText(Graphics2D g2) {
        boolean isReady = introState == IntroState.READY;
        String text = isReady ? "Ready..." : "Fight!!";
        Color col   = isReady ? new Color(230, 230, 100) : new Color(255, 80, 80);
        g2.setFont(new Font("SansSerif", Font.BOLD, 72));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (W - fm.stringWidth(text)) / 2, ty = H / 2 - 20;
        g2.setColor(new Color(0, 0, 0, introAlpha / 2));
        g2.drawString(text, tx + 3, ty + 3);
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), introAlpha));
        g2.drawString(text, tx, ty);
    }

    // ── Draw: floating text ───────────────────────────────────────────────────

    private void drawFloatingText(Graphics2D g2, FloatingText ft) {
        g2.setFont(new Font("SansSerif", ft.isMiss ? Font.BOLD | Font.ITALIC : Font.BOLD, ft.isCrit ? 20 : 15));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(ft.text);
        int dx = ft.leftAnchored ? (int) ft.x : (int) ft.x - tw / 2;
        g2.setColor(new Color(0, 0, 0, ft.alpha / 3));
        g2.drawString(ft.text, dx + 1, (int) ft.y + 1);
        Color c = ft.isMiss ? new Color(200,200,200,ft.alpha) : ft.isHeal ? new Color(80,230,80,ft.alpha)
                : ft.isPassiveDmg ? new Color(80,150,255,ft.alpha) : ft.isCrit ? new Color(255,215,0,ft.alpha)
                : new Color(255,80,80,ft.alpha);
        g2.setColor(c);
        g2.drawString(ft.text, dx, (int) ft.y);
    }

    // ── Draw: info overlay ────────────────────────────────────────────────────

    private void drawInfoOverlay(Graphics2D g2) {
        // Dim backdrop
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, W, H);

        java.util.List<Enemy> enemies = battle.getEnemyTeam();
        Enemy e = enemies.get(infoEnemyIndex);

        // ── Panel ─────────────────────────────────────────────────────────────
        int pw = 440, ph = 330;
        int px = (W - pw) / 2, py = (H - ph) / 2;

        g2.setColor(new Color(20, 20, 35, 245));
        g2.fillRoundRect(px, py, pw, ph, 16, 16);
        g2.setColor(new Color(120, 100, 60));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(px, py, pw, ph, 16, 16);
        g2.setStroke(new BasicStroke(1));

        // ── Title ─────────────────────────────────────────────────────────────
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fmT = g2.getFontMetrics();
        String title = "ENEMY INFO";
        g2.setColor(new Color(200, 180, 100));
        g2.drawString(title, px + (pw - fmT.stringWidth(title)) / 2, py + 22);
        g2.setColor(new Color(100, 80, 40, 140));
        g2.drawLine(px + 16, py + 30, px + pw - 16, py + 30);

        // ── Enemy name + status ───────────────────────────────────────────────
        String nameStr = e.getName() + (e.isAlive() ? "" : "  [DEFEATED]");
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        FontMetrics fmN = g2.getFontMetrics();
        g2.setColor(e.isAlive() ? Color.WHITE : new Color(140, 140, 140));
        g2.drawString(nameStr, px + (pw - fmN.stringWidth(nameStr)) / 2, py + 50);

        // ── Stats (two columns) ───────────────────────────────────────────────
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        FontMetrics fmS = g2.getFontMetrics();
        int c1 = px + 30, c2 = px + pw / 2 + 10, rowY = py + 70, gap = 22;

        String[][] left  = {
                { "HP",          e.getCurrentHp() + " / " + e.getMaxHp() },
                { "ATK",         String.valueOf(e.getAttack()) },
                { "DEF",         String.valueOf(e.getDefense()) },
        };
        String[][] right = {
                { "ATK Speed",   (e.getAttackSpeed() / 1000.0) + "s" },
                { "Crit Rate",   (int)(e.getCritRate()   * 100) + "%" },
                { "Crit Damage", "+" + (int)(e.getCritDamage() * 100) + "%" },
        };

        for (int i = 0; i < 3; i++) {
            int y = rowY + i * gap;
            g2.setColor(new Color(150, 150, 150)); g2.drawString(left[i][0]  + ":", c1, y);
            g2.setColor(Color.WHITE);               g2.drawString(left[i][1],       c1 + 80, y);
            g2.setColor(new Color(150, 150, 150)); g2.drawString(right[i][0] + ":", c2, y);
            g2.setColor(Color.WHITE);               g2.drawString(right[i][1],      c2 + 90, y);
        }

        // ── Passive section ───────────────────────────────────────────────────
        int divY = rowY + 3 * gap + 8;
        g2.setColor(new Color(100, 80, 40, 140));
        g2.drawLine(px + 16, divY, px + pw - 16, divY);

        Passive passive = e.getPassive();
        int passY = divY + 18;
        if (passive != null) {
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.setColor(new Color(150, 210, 255));
            g2.drawString("Passive:  " + passive.getName(), c1, passY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.setColor(new Color(190, 190, 190));
            g2.drawString(passive.getDescription(), c1, passY + 16);
        } else {
            g2.setFont(new Font("SansSerif", Font.ITALIC, 12));
            g2.setColor(new Color(110, 110, 110));
            g2.drawString("No passive ability.", c1, passY);
        }

        // ── Navigation ────────────────────────────────────────────────────────
        int navY = py + ph - 42;
        g2.setColor(new Color(80, 60, 30, 120));
        g2.drawLine(px + 16, navY - 8, px + pw - 16, navY - 8);

        // Counter
        String counter = (infoEnemyIndex + 1) + " / " + enemies.size();
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        FontMetrics fmC = g2.getFontMetrics();
        g2.setColor(new Color(170, 170, 170));
        g2.drawString(counter, px + (pw - fmC.stringWidth(counter)) / 2, navY + 16);

        // Left arrow
        boolean hasL = infoEnemyIndex > 0;
        int arrW = 34, arrH = 24, lax = px + 20, lay = navY;
        infoLeftArea.setBounds(lax, lay, arrW, arrH);
        g2.setColor(hasL ? (hoverLeft ? new Color(90,130,190) : new Color(50,70,120)) : new Color(35,35,45));
        g2.fillRoundRect(lax, lay, arrW, arrH, 6, 6);
        g2.setColor(hasL ? Color.WHITE : new Color(70, 70, 80));
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2.drawString("‹", lax + 10, lay + 17);

        // Right arrow
        boolean hasR = infoEnemyIndex < enemies.size() - 1;
        int rax = px + pw - 20 - arrW, ray = navY;
        infoRightArea.setBounds(rax, ray, arrW, arrH);
        g2.setColor(hasR ? (hoverRight ? new Color(90,130,190) : new Color(50,70,120)) : new Color(35,35,45));
        g2.fillRoundRect(rax, ray, arrW, arrH, 6, 6);
        g2.setColor(hasR ? Color.WHITE : new Color(70, 70, 80));
        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2.drawString("›", rax + 10, ray + 17);

        // Resume button
        int cbW = 88, cbH = 26, cbX = px + pw - cbW - 12, cbY = py + 6;
        infoCloseArea.setBounds(cbX, cbY, cbW, cbH);
        g2.setColor(hoverClose ? new Color(170,50,50,220) : new Color(90,25,25,200));
        g2.fillRoundRect(cbX, cbY, cbW, cbH, 8, 8);
        g2.setColor(new Color(255, 110, 110));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(cbX, cbY, cbW, cbH, 8, 8);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fmCl = g2.getFontMetrics();
        g2.setColor(Color.WHITE);
        g2.drawString("Resume", cbX + (cbW - fmCl.stringWidth("Resume")) / 2, cbY + 17);
    }

    // ── Draw: result overlay ──────────────────────────────────────────────────

    private void drawResultOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, overlayAlpha / 2));
        g2.fillRect(0, 0, W, H);
        if (battleResult == Battle.BattleState.PLAYER_WIN) drawVictoryOverlay(g2);
        else                                                drawDefeatOverlay(g2);
    }

    private void drawVictoryOverlay(Graphics2D g2) {
        java.util.List<java.util.Map.Entry<Character, Integer>> ranking = battle.getDamageRanking();
        int totalEnemyHp = battle.getTotalEnemyMaxHp();
        int rowH = 26, bw = 560;
        int bh = 80 + ranking.size() * rowH + 20 + 64;
        int bx = (W - bw) / 2, by = H / 2 - bh / 2;

        g2.setColor(new Color(15, 60, 20, overlayAlpha));
        g2.fillRoundRect(bx, by, bw, bh, 20, 20);
        g2.setColor(new Color(255, 255, 255, Math.min(overlayAlpha + 30, 255)));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(bx, by, bw, bh, 20, 20);
        g2.setStroke(new BasicStroke(1));

        int divY = by + 76;
        g2.setColor(new Color(255, 255, 255, overlayAlpha / 3));
        g2.drawLine(bx + 20, divY, bx + bw - 20, divY);

        // Headline
        String headline = "VICTORY";
        g2.setFont(new Font("SansSerif", Font.BOLD, 44));
        FontMetrics fmH = g2.getFontMetrics();
        int hx = bx + (bw - fmH.stringWidth(headline)) / 2, hy = by + 56;
        g2.setColor(new Color(0, 0, 0, overlayAlpha));
        g2.drawString(headline, hx + 2, hy + 2);
        g2.setColor(new Color(150, 255, 130, overlayAlpha));
        g2.drawString(headline, hx, hy);

        // Damage rows
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fmR = g2.getFontMetrics();
        int rowX = bx + 20, rowY = divY + 8;

        for (int i = 0; i < ranking.size(); i++) {
            java.util.Map.Entry<Character, Integer> entry = ranking.get(i);
            int    dmg    = entry.getValue();
            double pct    = totalEnemyHp > 0 ? dmg * 100.0 / totalEnemyHp : 0.0;
            String pctFmt = (pct % 1.0 == 0.0) ? String.valueOf((int) pct) : String.format("%.1f", pct);
            String rank   = ordinal(i + 1);
            String dmgStr = dmg + " damage";
            String pctStr = "(" + pctFmt + "%)";
            int cy = rowY + i * rowH + fmR.getAscent();

            Color rankColor = switch (i) {
                case 0  -> new Color(255, 210, 50,  overlayAlpha);
                case 1  -> new Color(180, 190, 200, overlayAlpha);
                case 2  -> new Color(200, 130, 70,  overlayAlpha);
                default -> new Color(180, 180, 180, overlayAlpha);
            };
            if (i == 0) {
                g2.setColor(new Color(255, 210, 50, overlayAlpha / 8));
                g2.fillRoundRect(rowX - 4, rowY + i * rowH, bw - 40 + 8, rowH - 2, 6, 6);
            }
            g2.setColor(rankColor);
            g2.drawString(rank + ":", rowX, cy);
            g2.setColor(new Color(230, 230, 230, overlayAlpha));
            g2.drawString(entry.getKey().getName(), rowX + 44, cy);
            int dmgX = bx + bw - 20 - fmR.stringWidth(pctStr) - 8 - fmR.stringWidth(dmgStr);
            g2.setColor(new Color(255, 140, 100, overlayAlpha));
            g2.drawString(dmgStr, dmgX, cy);
            g2.setColor(new Color(180, 220, 255, overlayAlpha));
            g2.drawString(pctStr, bx + bw - 20 - fmR.stringWidth(pctStr), cy);
        }

        // ── Exit / Next Floor buttons ─────────────────────────────────────────
        int statsBottom = divY + 8 + ranking.size() * rowH;
        g2.setColor(new Color(255, 255, 255, overlayAlpha / 4));
        g2.drawLine(bx + 20, statsBottom + 10, bx + bw - 20, statsBottom + 10);

        int btnW = 160, btnH = 36, btnY = statsBottom + 20, gap = 20;
        int btnStartX = bx + (bw - btnW * 2 - gap) / 2;

        // Exit
        int exitX = btnStartX;
        btnExit.setBounds(exitX, btnY, btnW, btnH);
        g2.setColor(hoverExitBtn ? new Color(150,45,45,overlayAlpha) : new Color(80,20,20,overlayAlpha));
        g2.fillRoundRect(exitX, btnY, btnW, btnH, 10, 10);
        g2.setColor(new Color(255, 120, 100, overlayAlpha));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(exitX, btnY, btnW, btnH, 10, 10);
        g2.setStroke(new BasicStroke(1));
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fmBtn = g2.getFontMetrics();
        g2.setColor(new Color(255, 255, 255, overlayAlpha));
        g2.drawString("Exit", exitX + (btnW - fmBtn.stringWidth("Exit")) / 2, btnY + btnH / 2 + 5);

        // Next Floor
        int nextX = btnStartX + btnW + gap;
        btnNextFloor.setBounds(nextX, btnY, btnW, btnH);
        g2.setColor(hoverNext ? new Color(30,120,50,overlayAlpha) : new Color(15,70,25,overlayAlpha));
        g2.fillRoundRect(nextX, btnY, btnW, btnH, 10, 10);
        g2.setColor(new Color(100, 220, 120, overlayAlpha));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(nextX, btnY, btnW, btnH, 10, 10);
        g2.setStroke(new BasicStroke(1));
        String nextStr = "Next Floor  ›";
        g2.setColor(new Color(255, 255, 255, overlayAlpha));
        g2.drawString(nextStr, nextX + (btnW - fmBtn.stringWidth(nextStr)) / 2, btnY + btnH / 2 + 5);
    }

    private void drawDefeatOverlay(Graphics2D g2) {
        int bw = 500, bh = 160, bx = (W - bw) / 2, by = H / 2 - bh / 2;
        g2.setColor(new Color(80, 20, 20, overlayAlpha));
        g2.fillRoundRect(bx, by, bw, bh, 20, 20);
        g2.setColor(new Color(255,255,255,Math.min(overlayAlpha + 30, 255)));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(bx, by, bw, bh, 20, 20);
        g2.setStroke(new BasicStroke(1));
        String headline = "DEFEAT";
        g2.setFont(new Font("SansSerif", Font.BOLD, 48));
        FontMetrics fmH = g2.getFontMetrics();
        int hx = bx + (bw - fmH.stringWidth(headline)) / 2, hy = by + bh / 2 + 4;
        g2.setColor(new Color(0, 0, 0, overlayAlpha));
        g2.drawString(headline, hx + 3, hy + 3);
        g2.setColor(new Color(255, 110, 90, overlayAlpha));
        g2.drawString(headline, hx, hy);
        String sub = "Fell on " + floor;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        FontMetrics fmS = g2.getFontMetrics();
        g2.setColor(new Color(210, 210, 210, overlayAlpha));
        g2.drawString(sub, bx + (bw - fmS.stringWidth(sub)) / 2, hy + fmH.getHeight() - 8);
    }

    private static String ordinal(int r) {
        return switch (r) { case 1 -> "1st"; case 2 -> "2nd"; case 3 -> "3rd"; default -> r + "th"; };
    }

    // ── Placeholder generators ────────────────────────────────────────────────

    private BufferedImage makePlaceholderSprite(Color base, String initial) {
        BufferedImage img = new BufferedImage(SPRITE_W, SPRITE_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int[] xs = {4, SPRITE_W-4, SPRITE_W-10, 10}, ys = {0, 0, SPRITE_H, SPRITE_H};
        g.setColor(base); g.fillPolygon(xs, ys, 4);
        g.setColor(base.darker()); g.setStroke(new BasicStroke(2)); g.drawPolygon(xs, ys, 4);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(255,255,255,200));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(initial, (SPRITE_W - fm.stringWidth(initial)) / 2, SPRITE_H / 2 + fm.getAscent() / 2 - 4);
        g.dispose(); return img;
    }

    private BufferedImage makePlaceholderBg() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        GradientPaint sky = new GradientPaint(0, 0, new Color(50, 35, 70), 0, GROUND_BASE - 60, new Color(110, 90, 55));
        g.setPaint(sky); g.fillRect(0, 0, W, GROUND_BASE - 60);
        int[] gx = {0, W, W, 0}, gy = {GROUND_BASE-60, GROUND_BASE-60, H, H};
        g.setPaint(new GradientPaint(0, GROUND_BASE-60, GROUND_FAR, 0, H, GROUND_NEAR));
        g.fillPolygon(gx, gy, 4);
        g.setColor(new Color(0, 0, 0, 30)); g.setStroke(new BasicStroke(1));
        int vp = W / 2;
        for (int i = -8; i <= 8; i++) g.drawLine(vp, GROUND_BASE-60, vp + i * 90, H);
        for (int r = 0; r <= 8; r++) {
            double t = r / 8.0;
            int y = (int)(GROUND_BASE - 60 + t * (H - (GROUND_BASE - 60)));
            g.drawLine((int)(vp - vp * t), y, (int)(vp + (W - vp) * t), y);
        }
        g.setFont(new Font("SansSerif", Font.BOLD | Font.ITALIC, 14));
        g.setColor(new Color(255, 255, 255, 55));
        String label = "[ Tower Stage Placeholder — replace with /res/tower_bg.gif ]";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(label, (W - fm.stringWidth(label)) / 2, GROUND_BASE - 70);
        g.dispose(); return img;
    }
}