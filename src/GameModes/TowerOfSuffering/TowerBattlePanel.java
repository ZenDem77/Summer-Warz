package GameModes.TowerOfSuffering;

import Combat.NormalBattle.Battle;
import Entities.Character;
import Entities.Enemy;
import Entities.Entity;
import Entities.PassiveHandler.*;
import Entities.Sprites.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

import Combat.BattleUI;
import Combat.BattleUI.FloatingText;

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
    private static final int    GROUND_BASE    = 580;
    private static final int    BASE_SPRITE_W  = 144;
    private static final int    BASE_SPRITE_H  = 180;
    /** Minimum pixel gap between the two fighters' sprite edges when fully engaged. */
    private static final int    GAP_PX        = 20;
    private static final int    APPROACH_SPEED = 5;

    private static final int PLAYER_START_X  = -500;
    private static final int ENEMY_START_X   =  500;
    private int playerTargetX = -55;
    private int enemyTargetX  =  55;
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

    // ── Attack pose ───────────────────────────────────────────────────────────
    private static final int ATTACK_POSE_MS = 200;
    private boolean playerAttacking = false, enemyAttacking = false;
    private int     playerAttackTick = 0,    enemyAttackTick = 0;

    // ── Frozen display references ───────────────────────────────────────────
    // See BattlePanel.java for full explanation: battle.getActivePlayer()/
    // getActiveEnemy() already point to the NEXT fighter the instant one
    // dies, so without freezing these, the dead fighter's sprite never gets
    // a chance to render during the pause before the next one slides in.
    private Entity displayedPlayer;
    private Entity displayedEnemy;
    private int     playerFlashTick = 0,    enemyFlashTick = 0;

    // ── Floating text ─────────────────────────────────────────────────────────
    // FloatingText itself is shared (see Combat.BattleUI.FloatingText, imported
    // above) so every panel's damage/heal/miss popups look and animate identically.
    private final List<FloatingText> floatingTexts = new ArrayList<>();

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
    private final Map<Entity, BufferedImage> runSpriteCache        = new HashMap<>();
    private final Map<Entity, BufferedImage> attackSpriteCache     = new HashMap<>();
    private final Map<Entity, BufferedImage> deadSpriteCache       = new HashMap<>();
    private final Map<Entity, BufferedImage> projectileSpriteCache = new HashMap<>();
    private final List<BattleUI.ProjectileSprite> projectiles       = new ArrayList<>();
    private final BufferedImage bgImage;

    // ─────────────────────────────────────────────────────────────────────────
    //  Constructor
    // ─────────────────────────────────────────────────────────────────────────

    public TowerBattlePanel(Battle battle, Floor floor, TowerBattleListener listener) {
        this.battle        = battle;
        this.floor         = floor;
        this.towerListener = listener;
        battle.addListener(this);

        // Reset the team's HP/alive state immediately, BEFORE any frame ever
        // renders. Without this, a character who died on the PREVIOUS floor
        // would still show as dead (0 HP, dead sprite) during this floor's
        // "Ready... Fight!!" intro — start() was previously only called once
        // the intro finished (see beginCombat()), but sprites/HUD render
        // every frame regardless of intro state, so the stale dead state was
        // visible for the whole intro window.
        battle.start();

        setPreferredSize(new Dimension(W, H));
        setLayout(null);

        BufferedImage loadedBg;
        try {
            String path = floor.isBoss()
                    ? "/boss_floor_bg.png"
                    : "/normal_floor_bg.png";
            loadedBg = javax.imageio.ImageIO.read(getClass().getResource(path));
        } catch (Exception e) {
            loadedBg = makePlaceholderBg();
        }
        bgImage = loadedBg;

        battle.getPlayerTeam().forEach(c -> spriteCache.put(c, loadSpriteOrPlaceholder(c, PLAYER_COL)));
        battle.getEnemyTeam() .forEach(e -> spriteCache.put(e, loadSpriteOrPlaceholder(e, ENEMY_COL)));
        battle.getPlayerTeam().forEach(c -> {
            BufferedImage dead = SpriteLoader.load(c.getSpriteSet().deadPath(), spriteWidthFor(c), spriteHeightFor(c));
            if (dead != null) deadSpriteCache.put(c, dead);
        });
        battle.getEnemyTeam().forEach(e -> {
            BufferedImage dead = SpriteLoader.load(e.getSpriteSet().deadPath(), spriteWidthFor(e), spriteHeightFor(e));
            if (dead != null) deadSpriteCache.put(e, dead);
        });
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
        battle.getPlayerTeam().forEach(c -> {
            BufferedImage proj = SpriteLoader.loadNative(c.getSpriteSet().projectilePath());
            if (proj != null) projectileSpriteCache.put(c, proj);
        });
        battle.getEnemyTeam().forEach(e -> {
            BufferedImage proj = SpriteLoader.loadNative(e.getSpriteSet().projectilePath());
            if (proj != null) projectileSpriteCache.put(e, proj);
        });

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
        // Set initialDelay to the full attack interval before restarting so
        // the first post-resume attack fires after a normal delay — not
        // immediately. Without this, timers started with setInitialDelay(0)
        // fire on the very next EDT cycle after start(), causing both fighters
        // to attack simultaneously the moment Resume is clicked (the "burst"
        // damage the player sees).
        if (playerAttackTimer != null) {
            playerAttackTimer.setInitialDelay(battle.getActivePlayer().getAttackSpeed());
            playerAttackTimer.start();
        }
        if (enemyAttackTimer != null) {
            enemyAttackTimer.setInitialDelay(battle.getActiveEnemy().getAttackSpeed());
            enemyAttackTimer.start();
        }
        battle.resume();
    }

    // ── Master tick ───────────────────────────────────────────────────────────

    private void tick() {
        if (!infoPanelOpen) {
            tickIntro();
            if (approaching) tickApproach();
            if (!approaching && combatStarted && !waitingForNext) bobTick++;
            if (!approaching && combatStarted && !waitingForNext) syncAttackTimers();
        }
        if (playerFlashing && ++playerFlashTick > HIT_FLASH_MS / TICK_MS) { playerFlashing = false; playerFlashTick = 0; }
        if (enemyFlashing  && ++enemyFlashTick > HIT_FLASH_MS / TICK_MS)  { enemyFlashing  = false; enemyFlashTick  = 0; }
        if (playerAttacking && ++playerAttackTick > ATTACK_POSE_MS / TICK_MS) { playerAttacking = false; playerAttackTick = 0; }
        if (enemyAttacking  && ++enemyAttackTick > ATTACK_POSE_MS / TICK_MS)  { enemyAttacking  = false; enemyAttackTick  = 0; }
        BattleUI.updateFloatingTexts(floatingTexts);
        BattleUI.updateProjectiles(projectiles);
        repaint();
    }

    /**
     * Keeps the running attack Timers in sync with each fighter's CURRENT
     * attack speed. Needed for passives like Lynx's "shield break → faster
     * attacks" which call Entity.setAttackSpeed() mid-battle.
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
        // battle.start() already ran in the constructor (see comment there) —
        // this just kicks off the actual slide-in/approach animation.
        combatStarted = true;
        approaching   = true;
        playerWorldX  = PLAYER_START_X;
        enemyWorldX   = ENEMY_START_X;
        computeTargetPositions();
    }

    /**
     * Computes where each fighter should stop, based on current sprite widths
     * plus a fixed gap. Called at every engagement start so a differently-sized
     * fighter entering mid-battle gets its own correct stop position.
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
            playerAttacking = true; playerAttackTick = 0;
            launchProjectile(true);
            if (isMiss) spawnMissPopup(false);
            else { enemyFlashing = true; enemyFlashTick = 0; showDmgPopup(false, damage, isCrit); }
        });
    }

    @Override
    public void onEnemyAttack(String log, int damage, boolean isCrit, boolean isMiss) {
        SwingUtilities.invokeLater(() -> {
            enemyAttacking = true; enemyAttackTick = 0;
            launchProjectile(false);
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
        // Set synchronously BEFORE invokeLater — same reasoning as BattlePanel:
        // pending render ticks fire before the lambda runs, and without this
        // being set immediately, getDisplayedPlayer/Enemy updates to the new
        // fighter (already advanced by Battle.java) before the freeze kicks in,
        // losing the dead-sprite display window entirely.
        waitingForNext = true;
        SwingUtilities.invokeLater(() -> {
            stopCombatTimers();
            new javax.swing.Timer(NEXT_ENTER_DELAY_MS, e -> {
                ((javax.swing.Timer) e.getSource()).stop();
                if (isPlayer) playerWorldX = PLAYER_START_X;
                else          enemyWorldX  = ENEMY_START_X;
                computeTargetPositions();
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

    private int spriteWidthFor(Entity entity)  { return (int)(BASE_SPRITE_W * entity.getSizeScale()); }
    private int spriteHeightFor(Entity entity) { return (int)(BASE_SPRITE_H * entity.getSizeScale()); }

    private Point spritePos(double worldX, double worldY, int spriteW, int spriteH) {
        return BattleUI.spritePos(W, GROUND_BASE, ISO_SCALE, worldX, worldY, spriteW, spriteH);
    }

    private void launchProjectile(boolean attackerIsPlayer) {
        Entity attacker = attackerIsPlayer ? getDisplayedPlayer() : getDisplayedEnemy();
        BufferedImage proj = projectileSpriteCache.get(attacker);
        if (proj == null) return;

        double startX  = attackerIsPlayer ? playerWorldX : enemyWorldX;
        double targetX = attackerIsPlayer ? enemyWorldX  : playerWorldX;
        int attackerW  = spriteWidthFor(attacker);
        Entity defender = attackerIsPlayer ? getDisplayedEnemy() : getDisplayedPlayer();
        int defenderW   = spriteWidthFor(defender);
        double launchX  = startX  + (attackerIsPlayer ?  attackerW / 2.0 : -attackerW / 2.0);
        double arriveX  = targetX + (attackerIsPlayer ? -defenderW / 2.0 :  defenderW / 2.0);
        double midY     = (PLAYER_WORLD_Y + ENEMY_WORLD_Y) / 2.0;

        projectiles.add(new BattleUI.ProjectileSprite(proj, launchX, arriveX, midY,
                BattleUI.ProjectileSprite.DEFAULT_SPEED_PX_PER_TICK));
    }

    private void showDmgPopup(boolean onPlayer, int dmg, boolean isCrit) {
        Entity entity = onPlayer ? getDisplayedPlayer() : getDisplayedEnemy();
        int sw = spriteWidthFor(entity), sh = spriteHeightFor(entity);
        Point sp = spritePos(onPlayer ? playerWorldX : enemyWorldX, onPlayer ? PLAYER_WORLD_Y : ENEMY_WORLD_Y, sw, sh);
        BattleUI.spawnDamagePopup(floatingTexts, sp.x, sp.y, sw, dmg, isCrit, TICK_MS);
    }

    private void spawnMissPopup(boolean onPlayer) {
        Entity entity = onPlayer ? getDisplayedPlayer() : getDisplayedEnemy();
        int sw = spriteWidthFor(entity), sh = spriteHeightFor(entity);
        Point sp = spritePos(onPlayer ? playerWorldX : enemyWorldX, onPlayer ? PLAYER_WORLD_Y : ENEMY_WORLD_Y, sw, sh);
        BattleUI.spawnMissPopup(floatingTexts, sp.x, sp.y, sw, TICK_MS);
    }

    private void spawnPassivePopup(boolean onPlayer, int amount, boolean isHeal) {
        Entity entity = onPlayer ? getDisplayedPlayer() : getDisplayedEnemy();
        int sw = spriteWidthFor(entity), sh = spriteHeightFor(entity);
        Point sp = spritePos(onPlayer ? playerWorldX : enemyWorldX, onPlayer ? PLAYER_WORLD_Y : ENEMY_WORLD_Y, sw, sh);
        BattleUI.spawnPassivePopup(floatingTexts, sp.x, sp.y, sw, sh, onPlayer, amount, isHeal, TICK_MS);
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

        Entity displayPlayer = getDisplayedPlayer();
        Entity displayEnemy  = getDisplayedEnemy();
        drawSprite(g2, displayPlayer, playerWorldX, PLAYER_WORLD_Y,
                getCurrentSprite(displayPlayer, playerAttacking, isPlayerMoving()), playerFlashing ? PLAYER_FLASH : null, false);
        drawSprite(g2, displayEnemy, enemyWorldX, ENEMY_WORLD_Y,
                getCurrentSprite(displayEnemy, enemyAttacking, isEnemyMoving()), enemyFlashing ? ENEMY_FLASH : null, true);

        // Projectiles drawn after sprites so they appear in front of fighters
        BattleUI.drawProjectiles(g2, projectiles, W, GROUND_BASE, ISO_SCALE);

        drawHud(g2);
        drawPlayerRoster(g2);
        drawEnemyCount(g2);

        for (FloatingText ft : floatingTexts) BattleUI.drawFloatingText(g2, ft);

        if (introState != IntroState.DONE)  drawIntroText(g2);
        if (infoPanelOpen)                  drawInfoOverlay(g2);
        if (battleResult != null)           drawResultOverlay(g2);
    }

    // ── Draw: sprite ──────────────────────────────────────────────────────────

    private void drawSprite(Graphics2D g2, Entity entity, double worldX, double worldY,
                            BufferedImage sprite, Color flashColor, boolean flipX) {
        BufferedImage deadSprite = deadSpriteCache.get(entity);
        boolean usingDeadSprite = !entity.isAlive() && deadSprite != null;
        if (usingDeadSprite) sprite = deadSprite;

        int spriteW = sprite.getWidth();
        int spriteH = sprite.getHeight();
        double scale = spriteW / (double) BASE_SPRITE_W;

        Point pos = spritePos(worldX, worldY, spriteW, spriteH);
        int sx = pos.x;
        int sy = pos.y + (!approaching && entity.isAlive() && !waitingForNext
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

        // Use the same frozen display references as the sprite (see
        // getDisplayedPlayer/getDisplayedEnemy) so the HP bar stays showing
        // the fighter that just died — at their actual last HP — through the
        // pause, instead of jumping straight to the next fighter's full HP
        // bar before the sprite has even changed.
        drawHudEntry(g2, getDisplayedPlayer(), BAR_MARGIN, false);
        drawHudEntry(g2, getDisplayedEnemy(),  W - BAR_MARGIN - BAR_W, true);

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
        BattleUI.drawHpBar(g2, entity, barX, 12, BAR_W, BAR_H, rightAlign,
                new BattleUI.HpBarColors(BAR_GREEN, BAR_YELLOW, BAR_RED, BAR_EMPTY));
    }

    // ── Draw: bottom strip ────────────────────────────────────────────────────

    private void drawPlayerRoster(Graphics2D g2) {
        List<Character> team = battle.getPlayerTeam();
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

    // ── Draw: info overlay ────────────────────────────────────────────────────

    private void drawInfoOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRect(0, 0, W, H);

        // ── Outer panel — wide enough to show both character and enemy ────────
        int pw = 880, ph = 370;
        int px = (W - pw) / 2, py = (H - ph) / 2;

        g2.setColor(new Color(20, 20, 35, 245));
        g2.fillRoundRect(px, py, pw, ph, 16, 16);
        g2.setColor(new Color(120, 100, 60));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(px, py, pw, ph, 16, 16);
        g2.setStroke(new BasicStroke(1));

        // Vertical divider between the two halves
        int divX = px + pw / 2;
        g2.setColor(new Color(100, 80, 40, 100));
        g2.drawLine(divX, py + 10, divX, py + ph - 10);

        // Header divider (below title row)
        g2.setColor(new Color(100, 80, 40, 140));
        g2.drawLine(px + 16, py + 32, px + pw - 16, py + 32);

        // ── Titles ────────────────────────────────────────────────────────────
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fmT = g2.getFontMetrics();
        int titleY = py + 22;

        String charTitle = "CHARACTER INFO";
        g2.setColor(new Color(100, 200, 130));
        g2.drawString(charTitle, px + (pw / 2 - fmT.stringWidth(charTitle)) / 2, titleY);

        String enemTitle = "ENEMY INFO";
        g2.setColor(new Color(200, 180, 100));
        g2.drawString(enemTitle, divX + (pw / 2 - fmT.stringWidth(enemTitle)) / 2, titleY);

        // Resume button (top-right corner of the whole panel)
        int cbW = 88, cbH = 26, cbX = px + pw - cbW - 12, cbY = py + 4;
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

        // ── Shared layout constants ────────────────────────────────────────────
        int halfW   = pw / 2;
        int margin  = 20;
        int contentMaxW = halfW - margin * 2;   // max text width per half
        int nameY   = py + 52;
        int statsY  = py + 74;
        int statGap = 20;
        int labelW  = 80;
        int valOff  = 85;

        // ── LEFT: Active character ─────────────────────────────────────────────
        {
            Character c = battle.getActivePlayer();
            int lx = px + margin;

            // Name
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            FontMetrics fmN = g2.getFontMetrics();
            String cName = c.getName() + (!c.isAlive() ? "  [DEFEATED]" : "");
            String cNameTrunc = cName;
            while (fmN.stringWidth(cNameTrunc) > contentMaxW && cNameTrunc.length() > 1)
                cNameTrunc = cNameTrunc.substring(0, cNameTrunc.length() - 1) + "…";
            g2.setColor(c.isAlive() ? Color.WHITE : new Color(140,140,140));
            g2.drawString(cNameTrunc, lx, nameY);

            // Stats (two-column within left half)
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            FontMetrics fmS = g2.getFontMetrics();
            int col2 = lx + halfW / 2 - margin / 2;

            String[][] cLeft = {
                    { "HP",       c.getCurrentHp() + " / " + c.getMaxHp() },
                    { "ATK",      String.valueOf(c.getEffectiveAtk()) },
                    { "DEF",      String.valueOf(c.getDefense()) },
            };
            String[][] cRight = {
                    { "Spd",      (c.getAttackSpeed() / 1000.0) + "s" },
                    { "CritRate", (int)(c.getCritRate() * 100) + "%" },
                    { "CritDmg",  "+" + (int)(c.getCritDamage() * 100) + "%" },
            };

            for (int i = 0; i < 3; i++) {
                int ry = statsY + i * statGap;
                g2.setColor(new Color(150, 150, 150)); g2.drawString(cLeft[i][0] + ":", lx, ry);
                g2.setColor(Color.WHITE);               g2.drawString(cLeft[i][1], lx + valOff, ry);
                g2.setColor(new Color(150, 150, 150)); g2.drawString(cRight[i][0] + ":", col2, ry);
                g2.setColor(Color.WHITE);               g2.drawString(cRight[i][1], col2 + valOff, ry);
            }

            // Passive divider
            int cDivY = statsY + 3 * statGap + 6;
            g2.setColor(new Color(100, 80, 40, 120));
            g2.drawLine(lx, cDivY, divX - margin, cDivY);

            // Character passives
            Passive[] slots = c.getPassives();
            int passY = cDivY + 14;
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            FontMetrics fmP = g2.getFontMetrics();
            boolean anyPassive = false;
            for (int i = 0; i < slots.length; i++) {
                if (slots[i] == null) continue;
                int unlockLevel = switch (i) {
                    case 0 -> Character.PASSIVE_1_LEVEL;
                    case 1 -> Character.PASSIVE_2_LEVEL;
                    default -> Character.PASSIVE_3_LEVEL;
                };
                boolean unlocked = c.getLevel() >= unlockLevel;
                anyPassive = true;

                // Passive name
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                g2.setColor(unlocked ? new Color(150, 210, 255) : new Color(100, 100, 120));
                String pLabel = (unlocked ? "" : "[Lv" + unlockLevel + "] ") + slots[i].getName();
                g2.drawString(pLabel, lx, passY);
                passY += 14;

                // Description — wrapped to stay within left half
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                fmP = g2.getFontMetrics();
                g2.setColor(unlocked ? new Color(190, 190, 190) : new Color(100, 100, 100));
                for (String line : BattleUI.wrapText(slots[i].getDescription(), fmP, contentMaxW)) {
                    g2.drawString(line, lx, passY);
                    passY += 13;
                }
                passY += 4;
            }
            if (!anyPassive) {
                g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
                g2.setColor(new Color(110, 110, 110));
                g2.drawString("No passives unlocked yet.", lx, passY);
            }
        }

        // ── RIGHT: Enemy ──────────────────────────────────────────────────────
        {
            List<Enemy> enemies = battle.getEnemyTeam();
            Enemy e = enemies.get(infoEnemyIndex);
            int rx = divX + margin;
            int rightMaxW = halfW - margin * 2 - 8;

            // Name
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            FontMetrics fmN = g2.getFontMetrics();
            String eName = e.getName() + (!e.isAlive() ? "  [DEFEATED]" : "");
            String eNameTrunc = eName;
            while (fmN.stringWidth(eNameTrunc) > rightMaxW && eNameTrunc.length() > 1)
                eNameTrunc = eNameTrunc.substring(0, eNameTrunc.length() - 1) + "…";
            g2.setColor(e.isAlive() ? Color.WHITE : new Color(140,140,140));
            g2.drawString(eNameTrunc, rx, nameY);

            // Stats
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            int col2 = rx + halfW / 2 - margin / 2;

            String[][] eLeft = {
                    { "HP",       e.getCurrentHp() + " / " + e.getMaxHp() },
                    { "ATK",      String.valueOf(e.getAttack()) },
                    { "DEF",      String.valueOf(e.getDefense()) },
            };
            String[][] eRight = {
                    { "Spd",      (e.getAttackSpeed() / 1000.0) + "s" },
                    { "CritRate", (int)(e.getCritRate() * 100) + "%" },
                    { "CritDmg",  "+" + (int)(e.getCritDamage() * 100) + "%" },
            };

            for (int i = 0; i < 3; i++) {
                int ry = statsY + i * statGap;
                g2.setColor(new Color(150, 150, 150)); g2.drawString(eLeft[i][0]  + ":", rx, ry);
                g2.setColor(Color.WHITE);               g2.drawString(eLeft[i][1],  rx + valOff, ry);
                g2.setColor(new Color(150, 150, 150)); g2.drawString(eRight[i][0] + ":", col2, ry);
                g2.setColor(Color.WHITE);               g2.drawString(eRight[i][1], col2 + valOff, ry);
            }

            // Passive divider
            int eDivY = statsY + 3 * statGap + 6;
            g2.setColor(new Color(100, 80, 40, 120));
            g2.drawLine(rx, eDivY, px + pw - margin, eDivY);

            // Enemy passive
            Passive ep = e.getPassive();
            int passY = eDivY + 14;
            if (ep != null) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                g2.setColor(new Color(255, 180, 100));
                g2.drawString(ep.getName(), rx, passY);
                passY += 14;

                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                FontMetrics fmP = g2.getFontMetrics();
                g2.setColor(new Color(190, 190, 190));
                for (String line : BattleUI.wrapText(ep.getDescription(), fmP, rightMaxW)) {
                    g2.drawString(line, rx, passY);
                    passY += 13;
                }
            } else {
                g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
                g2.setColor(new Color(110, 110, 110));
                g2.drawString("No passive ability.", rx, passY);
            }

            // ── Enemy navigation ───────────────────────────────────────────────
            int navY = py + ph - 42;
            g2.setColor(new Color(80, 60, 30, 120));
            g2.drawLine(rx, navY - 8, px + pw - margin, navY - 8);

            // Counter (centred over the right half)
            String counter = (infoEnemyIndex + 1) + " / " + enemies.size();
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            FontMetrics fmC = g2.getFontMetrics();
            g2.setColor(new Color(170, 170, 170));
            g2.drawString(counter, divX + (halfW - fmC.stringWidth(counter)) / 2, navY + 16);

            // Left arrow
            boolean hasL = infoEnemyIndex > 0;
            int arrW = 30, arrH = 22, lax = rx, lay = navY;
            infoLeftArea.setBounds(lax, lay, arrW, arrH);
            g2.setColor(hasL ? (hoverLeft ? new Color(90,130,190) : new Color(50,70,120)) : new Color(35,35,45));
            g2.fillRoundRect(lax, lay, arrW, arrH, 6, 6);
            g2.setColor(hasL ? Color.WHITE : new Color(70,70,80));
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("‹", lax + 9, lay + 16);

            // Right arrow
            boolean hasR = infoEnemyIndex < enemies.size() - 1;
            int rax = px + pw - margin - arrW, ray = navY;
            infoRightArea.setBounds(rax, ray, arrW, arrH);
            g2.setColor(hasR ? (hoverRight ? new Color(90,130,190) : new Color(50,70,120)) : new Color(35,35,45));
            g2.fillRoundRect(rax, ray, arrW, arrH, 6, 6);
            g2.setColor(hasR ? Color.WHITE : new Color(70,70,80));
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("›", rax + 9, ray + 16);
        }
    }

    // ── Draw: result overlay ──────────────────────────────────────────────────

    private void drawResultOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, overlayAlpha / 2));
        g2.fillRect(0, 0, W, H);
        if (battleResult == Battle.BattleState.PLAYER_WIN) drawVictoryOverlay(g2);
        else                                                drawDefeatOverlay(g2);
    }

    private void drawVictoryOverlay(Graphics2D g2) {
        List<Map.Entry<Character, Integer>> ranking = battle.getDamageRanking();
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
            String rank   = BattleUI.ordinal(i + 1);
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

    // ── Placeholder generators ────────────────────────────────────────────────

    /**
     * Frozen on the PREVIOUS fighter only during the pause BEFORE the next
     * fighter starts sliding in (waitingForNext=true, approaching=false) —
     * this is what shows the dead sprite briefly after a kill. The moment
     * the slide-in actually begins (approaching flips true), this switches
     * to the new fighter immediately, so its own run sprite gets selected
     * instead of the previous (dead) fighter's sprite bleeding into the new
     * fighter's entrance.
     */
    private Entity getDisplayedPlayer() {
        if (!waitingForNext || approaching) displayedPlayer = battle.getActivePlayer();
        return displayedPlayer;
    }

    private Entity getDisplayedEnemy() {
        if (!waitingForNext || approaching) displayedEnemy = battle.getActiveEnemy();
        return displayedEnemy;
    }

    /**
     * Whether the PLAYER side is actually sliding in right now. approaching
     * alone isn't enough — it's a single flag shared by both sides, so when
     * only the enemy is re-entering after a kill, approaching is true even
     * though the player isn't moving at all.
     */
    private boolean isPlayerMoving() { return approaching && playerWorldX != playerTargetX; }
    private boolean isEnemyMoving()  { return approaching && enemyWorldX  != enemyTargetX;  }

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
        return loaded != null ? loaded : BattleUI.makePlaceholderSprite(placeholderColor, entity.getName().substring(0, 1), w, h);
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