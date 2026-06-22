package Combat.NormalBattle;

import Entities.Character;
import Entities.Entity;
import Entities.PassiveHandler.*;
import Entities.Sprites.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * BattlePanel — full-screen battle view.
 *
 * Supports N characters (player dojo) vs M enemies.
 * Combat is always 1v1 between the active fighters; when one dies
 * the next slides in from off-screen before fighting resumes.
 *
 * Visual additions over the 1v1 panel:
 *  - Roster strip at the bottom of the HUD showing all team members
 *    (greyed out when dead, highlighted when active)
 */
public class BattlePanel extends JPanel implements Battle.BattleListener {

    // ── Window ────────────────────────────────────────────────────────────────
    private static final int W = 1280;
    private static final int H = 720;

    // ── Fighter geometry ──────────────────────────────────────────────────────
    private static final double ISO_SCALE     = 0.55;
    private static final int    GROUND_BASE   = 530;
    private static final int    BASE_SPRITE_W = 144;
    private static final int    BASE_SPRITE_H = 180;
    /** Minimum pixel gap between the two fighters' sprite edges when fully engaged. */
    private static final int    GAP_PX        = 20;
    private static final int    APPROACH_SPEED = 5;

    private static final int PLAYER_START_X = -500;
    private static final int ENEMY_START_X  =  500;
    // Computed dynamically in beginCombat() / setNextEngaged() from each
    // fighter's actual sprite width, so larger sprites automatically stop
    // further apart — no overlap regardless of entity size.
    private int playerTargetX = -55;
    private int enemyTargetX  =  55;
    private static final int PLAYER_WORLD_Y  = 60;
    private static final int ENEMY_WORLD_Y   = 90;

    // ── HUD ───────────────────────────────────────────────────────────────────
    private static final int HUD_H      = 70;
    private static final int BAR_W      = 320;
    private static final int BAR_H      = 18;
    private static final int BAR_MARGIN = 30;

    // Player roster (bottom-left)
    private static final int ICON_SIZE  = 28;
    private static final int ICON_GAP   = 8;
    private static final int ROSTER_X   = 20;   // left edge of roster
    private static final int ROSTER_Y   = H - 60; // top edge of roster

    // ── Timing ────────────────────────────────────────────────────────────────
    private static final int TICK_MS        = 16;
    private static final int READY_MS       = 1200;
    private static final int FIGHT_MS       = 800;
    private static final int HIT_FLASH_MS   = 100;
    private static final int BOB_AMPLITUDE  = 3;
    // Brief pause before the next fighter starts approaching
    private static final int NEXT_ENTER_DELAY_MS = 800;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final Color GROUND_NEAR  = new Color(130, 110, 70);
    private static final Color GROUND_FAR   = new Color(80, 90, 55);
    private static final Color PLAYER_COL   = new Color(50, 120, 240);
    private static final Color PLAYER_FLASH = new Color(180, 220, 255);
    private static final Color ENEMY_COL    = new Color(220, 50, 50);
    private static final Color ENEMY_FLASH  = new Color(255, 180, 160);
    private static final Color HUD_BG       = new Color(0, 0, 0, 160);
    private static final Color BAR_GREEN    = new Color(60, 200, 60);
    private static final Color BAR_YELLOW   = new Color(230, 190, 0);
    private static final Color BAR_RED      = new Color(210, 40, 40);
    private static final Color BAR_EMPTY    = new Color(40, 40, 40);

    // ── Battle ────────────────────────────────────────────────────────────────
    private final Battle battle;

    // ── Master render timer ───────────────────────────────────────────────────
    private final javax.swing.Timer renderTimer;

    // ── Intro ─────────────────────────────────────────────────────────────────
    private enum IntroState { READY, FIGHT, DONE }
    private IntroState introState = IntroState.READY;
    private int introAlpha = 0, introTick = 0;
    private static final int INTRO_FADE_TICKS = 12;
    private static final int READY_TICKS = READY_MS / TICK_MS;
    private static final int FIGHT_TICKS = FIGHT_MS / TICK_MS;

    // ── Attack timers ─────────────────────────────────────────────────────────
    private javax.swing.Timer playerAttackTimer;
    private javax.swing.Timer enemyAttackTimer;

    // ── World positions ───────────────────────────────────────────────────────
    private double  playerWorldX  = PLAYER_START_X;
    private double  enemyWorldX   = ENEMY_START_X;
    private boolean approaching   = false;
    private boolean combatStarted = false;
    private int     bobTick       = 0;

    // ── Flash ─────────────────────────────────────────────────────────────────
    private boolean playerFlashing = false, enemyFlashing = false;
    private int     playerFlashTick = 0,    enemyFlashTick = 0;

    // ── Attack pose ───────────────────────────────────────────────────────────
    // Briefly shows the attack sprite when an entity performs an attack tick,
    // then reverts to idle — same timer pattern as the hit-flash above.
    private static final int ATTACK_POSE_MS = 200;
    private boolean playerAttacking = false, enemyAttacking = false;
    private int     playerAttackTick = 0,    enemyAttackTick = 0;

    // ── Frozen display references ───────────────────────────────────────────
    // During the pause between a fighter dying and the next one sliding in
    // (waitingForNextFighter), battle.getActivePlayer()/getActiveEnemy()
    // ALREADY point to the next fighter (Battle.java advances the index
    // immediately on death). Without this, the dead fighter's sprite would
    // never actually get drawn — these freeze the displayed entity at its
    // last known value for the duration of that pause.
    private Entity displayedPlayer;
    private Entity displayedEnemy;

    // ── Floating text ─────────────────────────────────────────────────────────
    private final java.util.List<FloatingText> floatingTexts = new java.util.ArrayList<>();

    private static class FloatingText {
        static final int DURATION_MS = 700;
        static final int RISE_PX     = 40;
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
    private javax.swing.Timer overlayFadeTimer;

    // ── Sprite cache ──────────────────────────────────────────────────────────
    // One placeholder sprite per entity instance (color differs player vs enemy)
    private final Map<Entity, BufferedImage> spriteCache = new HashMap<>();
    private final Map<Entity, BufferedImage> runSpriteCache = new HashMap<>();
    private final Map<Entity, BufferedImage> attackSpriteCache = new HashMap<>();
    private final Map<Entity, BufferedImage> deadSpriteCache = new HashMap<>();
    private final BufferedImage bgImage;

    // ── Pending next-fighter state ────────────────────────────────────────────
    // Set when a fighter dies; cleared once the new fighter is fully engaged
    private boolean waitingForNextFighter = false;

    // ─────────────────────────────────────────────────────────────────────────

    public BattlePanel(Battle battle) {
        this.battle = battle;
        battle.addListener(this);

        // Reset HP/alive state immediately, before any frame ever renders —
        // see the matching comment in TowerBattlePanel's constructor for why.
        battle.start();

        setPreferredSize(new Dimension(W, H));
        setLayout(null);

        bgImage = makePlaceholderBg();
        // Pre-generate sprites for every fighter — tries real art first via
        // SpriteLoader (see SpritePaths.java to set file paths), falling back
        // to the placeholder shape if no path is set or the file can't be read.
        battle.getPlayerTeam().forEach(c -> spriteCache.put(c, loadSpriteOrPlaceholder(c, PLAYER_COL)));
        battle.getEnemyTeam().forEach(e -> spriteCache.put(e, loadSpriteOrPlaceholder(e, ENEMY_COL)));
        // Dead-pose sprites, where available — used in drawSprite() in place
        // of the placeholder X-cross overlay when an entity is defeated.
        battle.getPlayerTeam().forEach(c -> {
            BufferedImage dead = SpriteLoader.load(c.getSpriteSet().deadPath(), spriteWidthFor(c), spriteHeightFor(c));
            if (dead != null) deadSpriteCache.put(c, dead);
        });
        battle.getEnemyTeam().forEach(e -> {
            BufferedImage dead = SpriteLoader.load(e.getSpriteSet().deadPath(), spriteWidthFor(e), spriteHeightFor(e));
            if (dead != null) deadSpriteCache.put(e, dead);
        });
        // Run-pose sprites — used during the slide-in approach phase (see
        // the `approaching` boolean below), where available.
        battle.getPlayerTeam().forEach(c -> {
            BufferedImage run = SpriteLoader.load(c.getSpriteSet().runPath(), spriteWidthFor(c), spriteHeightFor(c));
            if (run != null) runSpriteCache.put(c, run);
        });
        battle.getEnemyTeam().forEach(e -> {
            BufferedImage run = SpriteLoader.load(e.getSpriteSet().runPath(), spriteWidthFor(e), spriteHeightFor(e));
            if (run != null) runSpriteCache.put(e, run);
        });
        battle.getPlayerTeam().forEach(c -> {
            BufferedImage attack = SpriteLoader.load(c.getSpriteSet().attackPath(), spriteWidthFor(c), spriteHeightFor(c));
            if (attack != null) attackSpriteCache.put(c, attack);
        });
        battle.getEnemyTeam().forEach(e -> {
            BufferedImage attack = SpriteLoader.load(e.getSpriteSet().attackPath(), spriteWidthFor(e), spriteHeightFor(e));
            if (attack != null) attackSpriteCache.put(e, attack);
        });

        renderTimer = new javax.swing.Timer(TICK_MS, e -> tick());
        renderTimer.start();
    }

    // ── Master tick ───────────────────────────────────────────────────────────

    private void tick() {
        tickIntro();
        if (approaching) tickApproach();
        if (!approaching && combatStarted && !waitingForNextFighter) bobTick++;
        if (!approaching && combatStarted && !waitingForNextFighter) syncAttackTimers();

        if (playerFlashing && ++playerFlashTick > HIT_FLASH_MS / TICK_MS) { playerFlashing = false; playerFlashTick = 0; }
        if (enemyFlashing  && ++enemyFlashTick > HIT_FLASH_MS / TICK_MS)  { enemyFlashing  = false; enemyFlashTick  = 0; }

        if (playerAttacking && ++playerAttackTick > ATTACK_POSE_MS / TICK_MS) { playerAttacking = false; playerAttackTick = 0; }
        if (enemyAttacking  && ++enemyAttackTick > ATTACK_POSE_MS / TICK_MS)  { enemyAttacking  = false; enemyAttackTick  = 0; }

        floatingTexts.removeIf(ft -> !ft.tick());
        repaint();
    }

    /**
     * Keeps the running attack Timers in sync with each fighter's CURRENT
     * attack speed. Needed for passives like Lynx's "shield break → faster
     * attacks" which call Entity.setAttackSpeed() mid-battle — the Timer
     * that was created with the OLD speed otherwise never finds out.
     */
    private void syncAttackTimers() {
        if (playerAttackTimer != null) {
            int currentSpeed = battle.getActivePlayer().getAttackSpeed();
            if (playerAttackTimer.getDelay() != currentSpeed) playerAttackTimer.setDelay(currentSpeed);
        }
        if (enemyAttackTimer != null) {
            int currentSpeed = battle.getActiveEnemy().getAttackSpeed();
            if (enemyAttackTimer.getDelay() != currentSpeed) enemyAttackTimer.setDelay(currentSpeed);
        }
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
        // battle.start() already ran in the constructor.
        combatStarted = true;
        approaching   = true;
        playerWorldX  = PLAYER_START_X;
        enemyWorldX   = ENEMY_START_X;
        computeTargetPositions();
    }

    /**
     * Computes where each fighter should stop sliding in, based on their
     * current sprite widths and a fixed gap between them. Called at the
     * start of every engagement (initial + each new-fighter entrance) so
     * a boss entering partway through the battle gets its own wider berth.
     *
     * Formula:
     *   total occupied width = (playerSpriteW + enemySpriteW) / 2 + GAP_PX
     *   player stops at  -(total / 2)
     *   enemy  stops at  +(total / 2)
     *
     * This guarantees the gap between the two sprites' inner edges
     * is always exactly GAP_PX, regardless of either sprite's size.
     */
    private void computeTargetPositions() {
        int pW = spriteWidthFor(battle.getActivePlayer());
        int eW = spriteWidthFor(battle.getActiveEnemy());
        int halfTotal = (pW / 2 + eW / 2 + GAP_PX) / 2;
        playerTargetX = -halfTotal;
        enemyTargetX  =  halfTotal;
    }

    // ── Approach ──────────────────────────────────────────────────────────────

    private void tickApproach() {
        if (!combatStarted) return;
        boolean pd = false, ed = false;
        if (playerWorldX < playerTargetX) playerWorldX = Math.min(playerWorldX + APPROACH_SPEED, playerTargetX); else pd = true;
        if (enemyWorldX  > enemyTargetX)  enemyWorldX  = Math.max(enemyWorldX  - APPROACH_SPEED, enemyTargetX);  else ed = true;

        if (pd && ed) {
            approaching = false;
            if (waitingForNextFighter) {
                // A new fighter just finished approaching — resume combat
                waitingForNextFighter = false;
                battle.setNextEngaged();
                restartCombatTimers();
            } else {
                // First engagement
                battle.setEngaged();
                startCombatTimers();
            }
        }
    }

    // ── Combat timers ─────────────────────────────────────────────────────────

    private void startCombatTimers() {
        playerAttackTimer = new javax.swing.Timer(battle.getActivePlayer().getAttackSpeed(), e -> battle.playerTick());
        playerAttackTimer.setInitialDelay(0);
        playerAttackTimer.start();

        enemyAttackTimer = new javax.swing.Timer(battle.getActiveEnemy().getAttackSpeed(), e -> battle.enemyTick());
        enemyAttackTimer.setInitialDelay(0);
        enemyAttackTimer.start();
    }

    private void stopCombatTimers() {
        if (playerAttackTimer != null) playerAttackTimer.stop();
        if (enemyAttackTimer  != null) enemyAttackTimer.stop();
    }

    /** Restart timers with possibly new active fighters after a team swap. */
    private void restartCombatTimers() {
        stopCombatTimers();
        startCombatTimers();
    }

    private void stopAllTimers() {
        renderTimer.stop();
        stopCombatTimers();
        if (overlayFadeTimer != null) overlayFadeTimer.stop();
        battle.stop();
    }

    // ── Battle.BattleListener ─────────────────────────────────────────────────

    @Override
    public void onPlayerAttack(String log, int damage, boolean isCrit, boolean isMiss) {
        SwingUtilities.invokeLater(() -> {
            playerAttacking = true; playerAttackTick = 0;
            if (isMiss) spawnMissPopup(false);
            else { enemyFlashing = true; enemyFlashTick = 0; showDmgPopup(false, damage, isCrit); }
        });
    }

    @Override
    public void onEnemyAttack(String log, int damage, boolean isCrit, boolean isMiss) {
        SwingUtilities.invokeLater(() -> {
            enemyAttacking = true; enemyAttackTick = 0;
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
    public void onPassiveMiss(String log, Entity owner, Entity target, String passiveName) {
        SwingUtilities.invokeLater(() -> {
            // Popup appears on the target (the one who avoided the hit) —
            // same convention as showDmgPopup/spawnMissPopup for normal attacks.
            boolean onPlayer = (owner != battle.getActivePlayer());
            spawnMissPopup(onPlayer);
        });
    }

    @Override
    public void onFighterEnter(boolean isPlayer, Entity fighter, int remaining) {
        // Set synchronously BEFORE invokeLater so the very next render tick
        // that fires (before the lambda even runs) already sees waitingForNextFighter=true
        // and freezes displayedPlayer/displayedEnemy on the dying fighter.
        // If this were inside the invokeLater, pending render ticks would fire
        // first and update the displayed entity to the NEW fighter (whose index
        // Battle.java already advanced to immediately on death), losing the dead
        // sprite window entirely.
        waitingForNextFighter = true;
        SwingUtilities.invokeLater(() -> {
            stopCombatTimers();

            // Brief pause, then slide the new fighter in from off-screen
            new javax.swing.Timer(NEXT_ENTER_DELAY_MS, e -> {
                ((javax.swing.Timer) e.getSource()).stop();
                if (isPlayer) {
                    playerWorldX = PLAYER_START_X;
                } else {
                    enemyWorldX = ENEMY_START_X;
                }
                // Recompute stop positions: the new fighter may be a
                // different size from the one that just died.
                computeTargetPositions();
                approaching = true;
            }) {{ setRepeats(false); start(); }};
        });
    }

    @Override
    public void onBattleEnd(Battle.BattleState result) {
        SwingUtilities.invokeLater(() -> {
            stopCombatTimers();
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
        Entity entity = onPlayer ? getDisplayedPlayer() : getDisplayedEnemy();
        int spriteW = spriteWidthFor(entity);
        Point sp = spritePos(onPlayer ? playerWorldX : enemyWorldX,
                onPlayer ? PLAYER_WORLD_Y : ENEMY_WORLD_Y, spriteW, spriteHeightFor(entity));
        if (dmg <= 0) {
            // Shield absorbed the entire hit — show BLOCK in blue
            floatingTexts.add(new FloatingText("BLOCK", sp.x + spriteW / 2f, sp.y - 10,
                    false, false, true, false, false));
            return;
        }
        String txt = "-" + dmg + (isCrit ? "!" : "");
        floatingTexts.add(new FloatingText(txt, sp.x + spriteW / 2f, sp.y - 10,
                isCrit, false, false, false, false));
    }

    private void spawnMissPopup(boolean onPlayer) {
        Entity entity = onPlayer ? getDisplayedPlayer() : getDisplayedEnemy();
        int spriteW = spriteWidthFor(entity);
        Point sp = spritePos(onPlayer ? playerWorldX : enemyWorldX,
                onPlayer ? PLAYER_WORLD_Y : ENEMY_WORLD_Y, spriteW, spriteHeightFor(entity));
        floatingTexts.add(new FloatingText("MISS!", sp.x + spriteW / 2f, sp.y - 10,
                false, false, false, true, false));
    }

    private void spawnPassivePopup(boolean onPlayer, int amount, boolean isHeal) {
        Entity entity = onPlayer ? getDisplayedPlayer() : getDisplayedEnemy();
        int spriteW = spriteWidthFor(entity);
        int spriteH = spriteHeightFor(entity);
        Point sp = spritePos(onPlayer ? playerWorldX : enemyWorldX,
                onPlayer ? PLAYER_WORLD_Y : ENEMY_WORLD_Y, spriteW, spriteH);
        String txt = (isHeal ? "+" : "-") + amount;
        boolean leftAnchored; float sx, sy;
        if (isHeal) {
            leftAnchored = !onPlayer;
            sx = onPlayer ? sp.x - 8 : sp.x + spriteW + 8;
            sy = sp.y + spriteH / 2f;
        } else {
            leftAnchored = false;
            sx = sp.x + spriteW / 2f;
            sy = sp.y - 10;
        }
        floatingTexts.add(new FloatingText(txt, sx, sy, false, isHeal, !isHeal, false, leftAnchored));
    }

    // ── Projection ────────────────────────────────────────────────────────────

    /**
     * This entity's sprite box width, scaled by its own getSizeScale()
     * relative to the base size. Normal entities (scale 1.0) get
     * BASE_SPRITE_W; a boss with scale 4.0 gets 4x that.
     */
    private int spriteWidthFor(Entity entity)  { return (int)(BASE_SPRITE_W * entity.getSizeScale()); }
    private int spriteHeightFor(Entity entity) { return (int)(BASE_SPRITE_H * entity.getSizeScale()); }

    /** Bottom-center anchor position for a sprite of the given size at this world position. */
    private Point spritePos(double worldX, double worldY, int spriteW, int spriteH) {
        return new Point(
                (int)(W / 2.0 + worldX) - spriteW / 2,
                (int)(GROUND_BASE - worldY * ISO_SCALE) - spriteH);
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

        // Draw active fighters — uses getDisplayedPlayer()/getDisplayedEnemy()
        // rather than battle.getActivePlayer()/getActiveEnemy() directly, so a
        // fighter that just died stays visible (including its dead sprite)
        // through the pause before the next fighter slides in. See the
        // displayedPlayer/displayedEnemy field comments for why this matters.
        Entity displayPlayer = getDisplayedPlayer();
        Entity displayEnemy  = getDisplayedEnemy();
        drawSprite(g2, displayPlayer, playerWorldX, PLAYER_WORLD_Y,
                getCurrentSprite(displayPlayer, playerAttacking, isPlayerMoving()),
                playerFlashing ? PLAYER_FLASH : null, false);
        drawSprite(g2, displayEnemy, enemyWorldX, ENEMY_WORLD_Y,
                getCurrentSprite(displayEnemy, enemyAttacking, isEnemyMoving()),
                enemyFlashing ? ENEMY_FLASH : null, true);

        drawHud(g2);
        drawPlayerRoster(g2);
        drawEnemyCount(g2);

        for (FloatingText ft : floatingTexts) drawFloatingText(g2, ft);

        if (introState != IntroState.DONE) drawIntroText(g2);
        if (battleResult != null)          drawResultOverlay(g2);
    }

    // ── Draw: sprite ──────────────────────────────────────────────────────────

    private void drawSprite(Graphics2D g2, Entity entity, double worldX, double worldY,
                            BufferedImage sprite, Color flashColor, boolean flipX) {
        // Swap in the dead-pose sprite once the entity is defeated, if one was loaded
        BufferedImage deadSprite = deadSpriteCache.get(entity);
        boolean usingDeadSprite = !entity.isAlive() && deadSprite != null;
        if (usingDeadSprite) sprite = deadSprite;

        // Every cached sprite (placeholder or loaded) is already sized to
        // this entity's own box at cache-population time (see
        // spriteWidthFor/spriteHeightFor), so we read dimensions straight
        // off the image rather than assuming a single shared size — this is
        // what lets bosses with getSizeScale() > 1.0 render correctly.
        int spriteW = sprite.getWidth();
        int spriteH = sprite.getHeight();
        // Scale factor relative to the base size — used to keep visual
        // margins (shadow, X-cross padding, etc.) proportionally consistent
        // regardless of how big or small this entity's sprite box is.
        double scale = spriteW / (double) BASE_SPRITE_W;

        Point pos = spritePos(worldX, worldY, spriteW, spriteH);
        int sx = pos.x;
        int sy = pos.y + (!approaching && entity.isAlive() && !waitingForNextFighter
                ? (int)(Math.sin(bobTick * 0.15 + (flipX ? Math.PI : 0)) * BOB_AMPLITUDE) : 0);

        int shadowY = (int)(GROUND_BASE - worldY * ISO_SCALE);
        int shadowMargin = (int)(8 * scale);
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillOval(sx + shadowMargin, shadowY - 8, Math.max(8, spriteW - shadowMargin * 2), 14);

        if (flipX) g2.drawImage(sprite, sx + spriteW, sy, -spriteW, spriteH, null);
        else       g2.drawImage(sprite, sx, sy, spriteW, spriteH, null);

        if (flashColor != null) {
            g2.setColor(new Color(flashColor.getRed(), flashColor.getGreen(), flashColor.getBlue(), 140));
            g2.fillRect(sx, sy, spriteW, spriteH);
        }

        // Fallback X-cross overlay — only shown when no dead sprite is available yet
        if (!entity.isAlive() && !usingDeadSprite) {
            g2.setColor(new Color(255, 255, 255, 200));
            g2.setStroke(new BasicStroke((float)(4 * scale), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int pad = (int)(14 * scale);
            g2.drawLine(sx + pad, sy + pad, sx + spriteW - pad, sy + spriteH - pad);
            g2.drawLine(sx + spriteW - pad, sy + pad, sx + pad, sy + spriteH - pad);
            g2.setStroke(new BasicStroke(1));
        }

        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        FontMetrics fm = g2.getFontMetrics();
        String name = entity.getName();
        int tw = fm.stringWidth(name);
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fillRoundRect(sx + spriteW / 2 - tw / 2 - 4, shadowY + 2, tw + 8, 14, 6, 6);
        g2.setColor(Color.WHITE);
        g2.drawString(name, sx + spriteW / 2 - tw / 2, shadowY + 13);
    }

    // ── Draw: HUD ─────────────────────────────────────────────────────────────

    private void drawHud(Graphics2D g2) {
        g2.setColor(HUD_BG);
        g2.fillRect(0, 0, W, HUD_H);

        // Active fighter HP bars
        drawHudEntry(g2, battle.getActivePlayer(), BAR_MARGIN, false);
        drawHudEntry(g2, battle.getActiveEnemy(),  W - BAR_MARGIN - BAR_W, true);

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

    /**
     * Draws the player's team roster at the bottom-left of the screen.
     * Each character gets an icon: gold = active, blue = waiting, dark = dead/already fought.
     * Called from paintComponent directly, not from drawHud.
     */
    private void drawPlayerRoster(Graphics2D g2) {
        java.util.List<Character> team = battle.getPlayerTeam();
        int activeIndex = battle.getPlayerIndex();

        // Background panel
        int panelW = team.size() * (ICON_SIZE + ICON_GAP) - ICON_GAP + 16;
        int panelH = ICON_SIZE + 28;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(ROSTER_X - 8, ROSTER_Y - 20, panelW, panelH, 10, 10);

        // Label
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.setColor(new Color(200, 200, 200));
        g2.drawString("YOUR TEAM", ROSTER_X, ROSTER_Y - 8);

        for (int i = 0; i < team.size(); i++) {
            Entity fighter = team.get(i);
            int x = ROSTER_X + i * (ICON_SIZE + ICON_GAP);
            int y = ROSTER_Y;

            boolean isActive = (i == activeIndex);
            boolean isDead   = !fighter.isAlive();
            boolean alreadyFought = i < activeIndex;

            Color fill = isDead || alreadyFought
                    ? new Color(40, 40, 40, 180)
                    : isActive
                    ? new Color(255, 210, 60, 230)
                    : new Color(60, 100, 180, 210);

            g2.setColor(fill);
            g2.fillRoundRect(x, y, ICON_SIZE, ICON_SIZE, 6, 6);

            g2.setColor(isActive ? new Color(255, 230, 80) : new Color(80, 80, 80));
            g2.setStroke(new BasicStroke(isActive ? 2f : 1f));
            g2.drawRoundRect(x, y, ICON_SIZE, ICON_SIZE, 6, 6);
            g2.setStroke(new BasicStroke(1));

            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            FontMetrics fm = g2.getFontMetrics();
            String init = fighter.getName().substring(0, 1);
            g2.setColor(isDead || alreadyFought ? new Color(100, 100, 100) : Color.WHITE);
            g2.drawString(init,
                    x + (ICON_SIZE - fm.stringWidth(init)) / 2,
                    y + (ICON_SIZE + fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    /**
     * Draws the enemy remaining count at the bottom-right of the screen.
     * Format: "Enemies remaining: N" (includes the currently active enemy).
     * Called from paintComponent directly, not from drawHud.
     */
    private void drawEnemyCount(Graphics2D g2) {
        int remaining = battle.getEnemyTeam().size() - battle.getEnemyIndex();
        String text   = "Enemies remaining: " + remaining;

        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        int panelW = tw + 24;
        int panelH = 34;
        int px = W - panelW - 20;          // 20px from right edge
        int py = H - panelH - 16;          // 16px from bottom edge

        // Background pill
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(px, py, panelW, panelH, 10, 10);

        // Text — red when multiple enemies left, orange when last one
        int tx = px + (panelW - tw) / 2;
        int ty = py + (panelH + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(text, tx + 1, ty + 1);
        Color col = remaining > 1 ? new Color(220, 100, 100) : new Color(255, 160, 60);
        g2.setColor(col);
        g2.drawString(text, tx, ty);
    }

    // ── Draw: intro ───────────────────────────────────────────────────────────

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
        g2.setFont(new Font("SansSerif", ft.isMiss ? Font.BOLD | Font.ITALIC : Font.BOLD,
                ft.isCrit ? 20 : 15));
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
        boolean won = battleResult == Battle.BattleState.PLAYER_WIN;

        // ── Dark veil ────────────────────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, overlayAlpha / 2));
        g2.fillRect(0, 0, W, H);

        if (won) {
            drawVictoryOverlay(g2);
        } else {
            drawDefeatOverlay(g2);
        }
    }

    /** Returns "1st", "2nd", "3rd", "4th", etc. */
    private static String ordinal(int rank) {
        return switch (rank) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> rank + "th";
        };
    }

    private void drawVictoryOverlay(Graphics2D g2) {
        java.util.List<java.util.Map.Entry<Character, Integer>> ranking = battle.getDamageRanking();
        int totalEnemyHp = battle.getTotalEnemyMaxHp();

        // Banner sized to fit headline + all character rows
        int rowH    = 26;
        int rows    = ranking.size();
        int bw      = 560;
        int bh      = 80 + rows * rowH + 20;   // headline area + rows + padding
        int bx      = (W - bw) / 2;
        int by      = H / 2 - bh / 2;

        // Banner background
        g2.setColor(new Color(15, 60, 20, overlayAlpha));
        g2.fillRoundRect(bx, by, bw, bh, 20, 20);
        g2.setColor(new Color(255, 255, 255, Math.min(overlayAlpha + 30, 255)));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(bx, by, bw, bh, 20, 20);
        g2.setStroke(new BasicStroke(1));

        // Divider line between headline and stats
        int dividerY = by + 76;
        g2.setColor(new Color(255, 255, 255, overlayAlpha / 3));
        g2.drawLine(bx + 20, dividerY, bx + bw - 20, dividerY);

        // ── Headline ─────────────────────────────────────────────────────────
        String headline = "VICTORY";
        g2.setFont(new Font("SansSerif", Font.BOLD, 44));
        FontMetrics fmH = g2.getFontMetrics();
        int hx = bx + (bw - fmH.stringWidth(headline)) / 2;
        int hy = by + 56;
        g2.setColor(new Color(0, 0, 0, overlayAlpha));
        g2.drawString(headline, hx + 2, hy + 2);
        g2.setColor(new Color(150, 255, 130, overlayAlpha));
        g2.drawString(headline, hx, hy);

        // ── Damage ranking rows ───────────────────────────────────────────────
        int rowX  = bx + 20;
        int rowY  = dividerY + 8;
        int colW  = bw - 40;

        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fmR = g2.getFontMetrics();

        for (int i = 0; i < ranking.size(); i++) {
            java.util.Map.Entry<Character, Integer> entry = ranking.get(i);
            int    dmg    = entry.getValue();
            double pct    = totalEnemyHp > 0 ? dmg * 100.0 / totalEnemyHp : 0.0;
            // Show whole numbers without decimal, otherwise 1 decimal place
            String pctFmt = (pct % 1.0 == 0.0)
                    ? String.valueOf((int) pct)
                    : String.format("%.1f", pct);
            String rank   = ordinal(i + 1);
            String name   = entry.getKey().getName();
            String dmgStr = dmg + " damage";
            String pctStr = "(" + pctFmt + "%)";

            int cy = rowY + i * rowH + fmR.getAscent();

            // Rank — gold for 1st, silver for 2nd, bronze for 3rd, white for rest
            Color rankColor = switch (i) {
                case 0 -> new Color(255, 210, 50,  overlayAlpha);
                case 1 -> new Color(180, 190, 200, overlayAlpha);
                case 2 -> new Color(200, 130, 70,  overlayAlpha);
                default -> new Color(180, 180, 180, overlayAlpha);
            };

            // Row highlight for 1st place
            if (i == 0) {
                g2.setColor(new Color(255, 210, 50, overlayAlpha / 8));
                g2.fillRoundRect(rowX - 4, rowY + i * rowH, colW + 8, rowH - 2, 6, 6);
            }

            // Rank label
            g2.setColor(rankColor);
            g2.drawString(rank + ":", rowX, cy);

            // Character name
            int nameX = rowX + 44;
            g2.setColor(new Color(230, 230, 230, overlayAlpha));
            g2.drawString(name, nameX, cy);

            // Damage — right side
            int dmgX = bx + bw - 20 - fmR.stringWidth(pctStr) - 8 - fmR.stringWidth(dmgStr);
            g2.setColor(new Color(255, 140, 100, overlayAlpha));
            g2.drawString(dmgStr, dmgX, cy);

            // Percentage — far right
            int pctX = bx + bw - 20 - fmR.stringWidth(pctStr);
            g2.setColor(new Color(180, 220, 255, overlayAlpha));
            g2.drawString(pctStr, pctX, cy);
        }
    }

    private void drawDefeatOverlay(Graphics2D g2) {
        int bw = 500, bh = 160, bx = (W - bw) / 2, by = H / 2 - bh / 2;
        g2.setColor(new Color(80, 20, 20, overlayAlpha));
        g2.fillRoundRect(bx, by, bw, bh, 20, 20);
        g2.setColor(new Color(255, 255, 255, Math.min(overlayAlpha + 30, 255)));
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

        String sub = battle.getEnemyTeam().get(battle.getEnemyIndex()).getName() + " wins!";
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        FontMetrics fmS = g2.getFontMetrics();
        g2.setColor(new Color(210, 210, 210, overlayAlpha));
        g2.drawString(sub, bx + (bw - fmS.stringWidth(sub)) / 2, hy + fmH.getHeight() - 8);
    }

    // ── Placeholder generators ────────────────────────────────────────────────

    /**
     * Tries to load this entity's idle sprite via SpriteLoader (see
     * SpritePaths.java to set file paths). Falls back to the generated
     * placeholder shape if no path is set or the file can't be read.
     */
    /**
     * Picks the sprite to draw this frame: run-pose while the fighter is
     * sliding in during the approach phase (if a run sprite was loaded),
     * otherwise the normal idle sprite.
     */
    /**
     * Picks the sprite to draw this frame, in priority order:
     *   1. Attack pose — briefly shown right after this side's attack tick fires
     *      (if an attack sprite was loaded; see playerAttacking/enemyAttacking)
     *   2. Run pose — while sliding in during the approach phase
     *   3. Idle — the default
     */
    /**
     * Returns the player entity to draw this frame.
     *
     * Frozen on the PREVIOUS fighter only during the pause BEFORE the next
     * fighter starts sliding in (waitingForNextFighter=true, approaching=false)
     * — this is what shows the dead sprite briefly after a kill. The moment
     * the slide-in animation actually begins (approaching flips true), this
     * switches to the new fighter immediately, so its own run sprite gets
     * selected instead of the previous (dead) fighter's sprite bleeding into
     * the new fighter's entrance.
     */
    private Entity getDisplayedPlayer() {
        if (!waitingForNextFighter || approaching) displayedPlayer = battle.getActivePlayer();
        return displayedPlayer;
    }

    private Entity getDisplayedEnemy() {
        if (!waitingForNextFighter || approaching) displayedEnemy = battle.getActiveEnemy();
        return displayedEnemy;
    }

    /**
     * Whether the PLAYER side is actually sliding in right now. approaching
     * alone isn't enough — it's a single flag shared by both sides, so when
     * only the enemy is re-entering after a kill, approaching is true even
     * though the player isn't moving at all. Comparing live position against
     * the target catches this: a side that's already at its target isn't
     * actually moving, regardless of the shared flag's state.
     */
    private boolean isPlayerMoving() { return approaching && playerWorldX != playerTargetX; }
    private boolean isEnemyMoving()  { return approaching && enemyWorldX  != enemyTargetX;  }

    /**
     * Picks the sprite to draw this frame, in priority order:
     *   1. Attack pose — briefly shown right after this side's attack tick fires
     *   2. Run pose — while THIS SIDE is actually sliding in (see isPlayerMoving/
     *      isEnemyMoving — not the shared approaching flag, which is true
     *      even when only the OTHER side is moving)
     *   3. Idle — the default
     */
    private BufferedImage getCurrentSprite(Entity entity, boolean isAttacking, boolean isMoving) {
        if (isAttacking) {
            BufferedImage attack = attackSpriteCache.get(entity);
            if (attack != null) return attack;
        }
        if (isMoving) {
            BufferedImage run = runSpriteCache.get(entity);
            if (run != null) return run;
        }
        return spriteCache.get(entity);
    }

    private BufferedImage loadSpriteOrPlaceholder(Entity entity, Color placeholderColor) {
        int w = spriteWidthFor(entity), h = spriteHeightFor(entity);
        BufferedImage loaded = SpriteLoader.load(entity.getSpriteSet().idlePath(), w, h);
        return loaded != null ? loaded : makePlaceholderSprite(placeholderColor, entity.getName().substring(0, 1), w, h);
    }

    private BufferedImage makePlaceholderSprite(Color base, String initial, int spriteW, int spriteH) {
        BufferedImage img = new BufferedImage(spriteW, spriteH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int[] xs = { 4, spriteW - 4, spriteW - 10, 10 };
        int[] ys = { 0, 0, spriteH, spriteH };
        g.setColor(base); g.fillPolygon(xs, ys, 4);
        g.setColor(base.darker()); g.setStroke(new BasicStroke(2)); g.drawPolygon(xs, ys, 4);
        g.setFont(new Font("SansSerif", Font.BOLD, Math.max(12, spriteW / 3)));
        g.setColor(new Color(255, 255, 255, 200));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(initial, (spriteW - fm.stringWidth(initial)) / 2, spriteH / 2 + fm.getAscent() / 2 - 4);
        g.dispose();
        return img;
    }

    private BufferedImage makePlaceholderBg() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        GradientPaint sky = new GradientPaint(0, 0, new Color(100, 130, 180), 0, GROUND_BASE - 60, new Color(160, 185, 140));
        g.setPaint(sky); g.fillRect(0, 0, W, GROUND_BASE - 60);
        int[] gx = {0, W, W, 0}, gy = {GROUND_BASE-60, GROUND_BASE-60, H, H};
        GradientPaint gnd = new GradientPaint(0, GROUND_BASE-60, GROUND_FAR, 0, H, GROUND_NEAR);
        g.setPaint(gnd); g.fillPolygon(gx, gy, 4);
        g.setColor(new Color(0, 0, 0, 25)); g.setStroke(new BasicStroke(1));
        int vp = W / 2;
        for (int i = -8; i <= 8; i++) g.drawLine(vp, GROUND_BASE-60, vp + i * 90, H);
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
}