package Combat;

import Entities.Entity;
import Entities.PassiveHandler.Shielded;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * BattleUI — shared rendering helpers used by every battle panel
 * (BattlePanel, TowerBattlePanel, CharacterBattlePanel, and any future
 * battle panel).
 *
 * This class exists so that visual changes to things like damage popups,
 * the shield bar, or placeholder sprites only need to be made in ONE place
 * instead of copy-pasted across every panel.
 *
 * What lives here vs. what stays in each panel:
 *   HERE   — methods that were byte-for-byte (or near-identical) duplicated
 *            across all three panels: floating combat text, HP/shield bar,
 *            placeholder sprite generation, sprite-box positioning, text
 *            wrapping, ordinal formatting.
 *   PANEL  — drawSprite() (the main character render with bob/flash/dead-
 *            sprite swapping) intentionally stays in each panel. It's
 *            tightly coupled to each panel's own animation-state fields
 *            (approaching, waitingForNextFighter, bobTick, etc.), and each
 *            panel's timing/freeze logic was hard-won through several bug
 *            fixes — merging it here would risk reintroducing those bugs
 *            for a relatively small duplication payoff. Floor labels, info
 *            overlays, victory/defeat screens, and roster strips are also
 *            panel-specific and stay where they are.
 *
 * All methods are static — this class is a toolbox, not an object you
 * instantiate. Pass in whatever panel-specific state (Graphics2D, colors,
 * positions, tick rate) each method needs.
 */
public class BattleUI {

    private BattleUI() {} // never instantiated

    // ── Floating combat text ─────────────────────────────────────────────────
    public static class FloatingText {
        static final int DURATION_MS = 700;
        static final int RISE_PX     = 40;
        public String text; public float x, y; public int alpha = 255;
        public boolean isCrit, isHeal, isPassiveDmg, isMiss, leftAnchored, isSpecialHit, isImmune;
        int ticksLeft; float dy; int dAlpha;

        public FloatingText(String text, float x, float y,
                            boolean isCrit, boolean isHeal, boolean isPassiveDmg,
                            boolean isMiss, boolean leftAnchored, int tickMs) {
            this(text, x, y, isCrit, isHeal, isPassiveDmg, isMiss, leftAnchored, false, tickMs);
        }

        public FloatingText(String text, float x, float y,
                            boolean isCrit, boolean isHeal, boolean isPassiveDmg,
                            boolean isMiss, boolean leftAnchored, boolean isSpecialHit, int tickMs) {
            this.text = text; this.x = x; this.y = y;
            this.isCrit = isCrit; this.isHeal = isHeal;
            this.isPassiveDmg = isPassiveDmg; this.isMiss = isMiss;
            this.leftAnchored = leftAnchored; this.isSpecialHit = isSpecialHit;
            int total = DURATION_MS / tickMs;
            ticksLeft = total; dy = (float) RISE_PX / total; dAlpha = 255 / total;
        }

        /** Advances one tick. Returns false once this text should be removed. */
        public boolean tick() { y -= dy; alpha -= dAlpha; ticksLeft--; return alpha > 0 && ticksLeft > 0; }
    }

    /** Advances every FloatingText in the list by one tick and removes expired ones. */
    public static void updateFloatingTexts(List<FloatingText> texts) {
        texts.removeIf(ft -> !ft.tick());
    }

    /** Draws a single FloatingText with its drop shadow and color-coded fill. */
    public static void drawFloatingText(Graphics2D g2, FloatingText ft) {
        int fontSize = (ft.isSpecialHit && ft.isCrit) ? 24
                : ft.isCrit                      ? 20
                : 15;
        int style = (ft.isMiss || ft.isImmune) ? Font.BOLD | Font.ITALIC : Font.BOLD;
        g2.setFont(new Font("SansSerif", style, fontSize));
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(ft.text);
        int dx = ft.leftAnchored ? (int) ft.x : (int) ft.x - tw / 2;
        g2.setColor(new Color(0, 0, 0, ft.alpha / 3));
        g2.drawString(ft.text, dx + 1, (int) ft.y + 1);
        Color c = ft.isImmune                    ? new Color(0,   220, 220, ft.alpha)  // cyan
                : ft.isMiss                      ? new Color(200, 200, 200, ft.alpha)  // grey
                : ft.isHeal                      ? new Color(80,  230, 80,  ft.alpha)  // green
                : ft.isSpecialHit && ft.isCrit   ? new Color(255, 215, 0,   ft.alpha)  // gold
                : ft.isSpecialHit                ? new Color(255, 80,  80,  ft.alpha)  // red
                : ft.isPassiveDmg                ? new Color(80,  150, 255, ft.alpha)  // blue
                : ft.isCrit                      ? new Color(255, 215, 0,   ft.alpha)  // gold
                :                                  new Color(255, 80,  80,  ft.alpha); // red
        g2.setColor(c);
        g2.drawString(ft.text, dx, (int) ft.y);
    }

    // ── Popup spawners ───────────────────────────────────────────────────────
    // These append a new FloatingText to the given list, positioned relative
    // to a sprite box (spriteX, spriteY = top-left corner; spriteW, spriteH =
    // box size). Pass entity.isAlive() / onPlayer-style flags from the caller.

    /** Spawns a damage number (or "BLOCK" if dmg<=0, meaning a shield absorbed the hit). */
    public static void spawnDamagePopup(List<FloatingText> texts, int spriteX, int spriteY,
                                        int spriteW, int dmg, boolean isCrit, int tickMs) {
        if (dmg <= 0) {
            texts.add(new FloatingText("BLOCK", spriteX + spriteW / 2f, spriteY - 10,
                    false, false, true, false, false, tickMs));
            return;
        }
        String txt = "-" + dmg + (isCrit ? "!" : "");
        texts.add(new FloatingText(txt, spriteX + spriteW / 2f, spriteY - 10,
                isCrit, false, false, false, false, tickMs));
    }

    /** Spawns a "MISS!" popup. */
    public static void spawnMissPopup(List<FloatingText> texts, int spriteX, int spriteY,
                                      int spriteW, int tickMs) {
        texts.add(new FloatingText("MISS!", spriteX + spriteW / 2f, spriteY - 10,
                false, false, false, true, false, tickMs));
    }

    /**
     * Spawns an "IMMUNE" popup — shown when a true-damage passive is blocked
     * by the target's immunity. Cyan color distinguishes it from MISS (grey).
     */
    public static void spawnImmunePopup(List<FloatingText> texts, int spriteX, int spriteY,
                                        int spriteW, int tickMs) {
        FloatingText ft = new FloatingText("IMMUNE", spriteX + spriteW / 2f, spriteY - 10,
                false, false, false, false, false, tickMs);
        ft.isImmune = true;
        texts.add(ft);
    }

    /**
     * Spawns a passive heal/damage popup. Heal popups anchor to the side of
     * the sprite (left or right, depending on which side onPlayer/onTarget
     * is on); damage popups float up from the top like a normal hit.
     */
    public static void spawnPassivePopup(List<FloatingText> texts, int spriteX, int spriteY,
                                         int spriteW, int spriteH, boolean onPlayer,
                                         int amount, boolean isHeal, int tickMs) {
        String txt = (isHeal ? "+" : "-") + amount;
        boolean leftAnchored; float sx, sy;
        if (isHeal) {
            leftAnchored = !onPlayer;
            sx = onPlayer ? spriteX - 8 : spriteX + spriteW + 8;
            sy = spriteY + spriteH / 2f;
        } else {
            leftAnchored = false;
            sx = spriteX + spriteW / 2f;
            sy = spriteY - 10;
        }
        texts.add(new FloatingText(txt, sx, sy, false, isHeal, !isHeal, false, leftAnchored, tickMs));
    }

    /**
     * Spawns a special-hit popup — used for abilities like Zenzenkoi's
     * Last Stand Strike that should visually stand out from generic passive damage.
     *
     * Colors:    crit  → gold,  no crit → red
     * Text size: crit  → large, no crit → normal (same as a regular hit)
     * Text format: "dmg!" on crit, "dmg" otherwise (caller appends the "!")
     */
    public static void spawnSpecialHitPopup(List<FloatingText> texts, int spriteX, int spriteY,
                                            int spriteW, int dmg, boolean isCrit, int tickMs) {
        String txt = "-" + dmg + (isCrit ? "!" : "");
        texts.add(new FloatingText(txt, spriteX + spriteW / 2f, spriteY - 10,
                isCrit, false, false, false, false, true, tickMs));
    }

    // ── Sprite positioning ───────────────────────────────────────────────────

    /**
     * Bottom-center anchor position for a sprite of the given size at this
     * world position, projected onto the panel's isometric ground plane.
     */
    public static Point spritePos(int panelW, int groundBase, double isoScale,
                                  double worldX, double worldY, int spriteW, int spriteH) {
        return new Point(
                (int)(panelW / 2.0 + worldX) - spriteW / 2,
                (int)(groundBase - worldY * isoScale) - spriteH);
    }

    // ── Placeholder sprite ────────────────────────────────────────────────────

    /**
     * Generates a simple colored placeholder sprite (a trapezoid with the
     * entity's first initial) — used when no real sprite art is loaded yet.
     */
    public static BufferedImage makePlaceholderSprite(Color base, String initial, int spriteW, int spriteH) {
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

    // ── HP / shield bar ───────────────────────────────────────────────────────

    /** Colors used by drawHpBar — passed in so panels keep control of their palette. */
    public record HpBarColors(Color green, Color yellow, Color red, Color empty) {}

    /**
     * Draws a complete HUD entry: name, HP bar (color-coded by percent),
     * HP text, and — if the entity implements Shielded and has shield HP —
     * a blue shield overlay bar with "Shield: N" text below.
     *
     * @param barX       left edge of the bar
     * @param barY       top edge of the name text
     * @param barW       bar width
     * @param barH       bar height
     * @param rightAlign true to right-align name/HP/shield text (for the enemy side)
     */
    public static void drawHpBar(Graphics2D g2, Entity entity, int barX, int barY,
                                 int barW, int barH, boolean rightAlign, HpBarColors colors) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        String name = entity.getName();
        int nx = rightAlign ? barX + barW - fm.stringWidth(name) : barX;
        g2.setColor(Color.WHITE);
        g2.drawString(name, nx, barY + 11);

        int by = barY + 16;
        g2.setColor(colors.empty());
        g2.fillRoundRect(barX, by, barW, barH, barH, barH);
        double pct = entity.getHpPercent();
        int fillW = Math.max(0, (int)(barW * pct));

        // Color thresholds:
        //   HP > 50%          → green
        //   25% < HP <= 50%   → orange
        //   HP <= 25%         → red
        Color barCol = pct > 0.50 ? new Color(60,  200, 60)   // green
                : pct > 0.25 ? new Color(230, 130, 0)    // orange
                :              new Color(210, 40,  40);   // red

        if (fillW > 0) {
            g2.setColor(barCol);
            g2.fillRoundRect(barX, by, fillW, barH, barH, barH);
            g2.setColor(new Color(255, 255, 255, 50));
            g2.fillRoundRect(barX, by, fillW, barH / 2, barH, barH);
        }
        g2.setColor(new Color(0, 0, 0, 120));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(barX, by, barW, barH, barH, barH);
        g2.setStroke(new BasicStroke(1));

        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        fm = g2.getFontMetrics();
        String hp = entity.getCurrentHp() + " / " + entity.getMaxHp();
        int hx = rightAlign ? barX + barW - fm.stringWidth(hp) : barX;
        g2.setColor(new Color(220, 220, 220));
        g2.drawString(hp, hx, by + barH + 13);

        if (entity instanceof Shielded s && s.getShieldHp() > 0) {
            double shieldPct = Math.min(1.0, (double) s.getShieldHp() / entity.getMaxHp());
            int shieldW = Math.max(4, (int)(barW * shieldPct));
            g2.setColor(new Color(80, 130, 255, 150));
            g2.fillRoundRect(barX, by, shieldW, barH, barH, barH);
            g2.setColor(new Color(160, 190, 255, 60));
            g2.fillRoundRect(barX, by, shieldW, barH / 2, barH, barH);

            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            fm = g2.getFontMetrics();
            String shieldStr = "Shield: " + s.getShieldHp();
            int shx = rightAlign ? barX + barW - fm.stringWidth(shieldStr) : barX;
            g2.setColor(new Color(130, 170, 255));
            g2.drawString(shieldStr, shx, by + barH + 26);
        }
    }

    // ── Projectile sprites ────────────────────────────────────────────────────

    /**
     * ProjectileSprite — a single in-flight projectile traveling from the
     * attacker toward the defender along a fixed horizontal path.
     *
     * Launched in the panel's onPlayerAttack/onEnemyAttack callback (so it
     * fires at the same moment the damage number pops up) and drawn each
     * tick until it passes the target's center X, at which point it removes
     * itself.
     *
     * Travel speed is in world-pixels per render tick (16ms). At 12 px/tick
     * and ~110px between fighters at the default engage gap, flight time is
     * roughly 150ms — matching the ATTACK_POSE_MS window nicely.
     */
    public static class ProjectileSprite {

        /** Default travel speed in world-X pixels per render tick (16ms). */
        public static final double DEFAULT_SPEED_PX_PER_TICK = 12.0;

        public final BufferedImage image;
        /** Current world-X center of the projectile. */
        public double worldX;
        /** World-Y drawn at (constant — halfway between PLAYER_WORLD_Y and ENEMY_WORLD_Y). */
        public final double worldY;
        /** Positive = flying right (player → enemy), negative = flying left (enemy → player). */
        public final double velocityX;
        /** World-X the projectile is flying toward; removed once it passes this. */
        public final double targetWorldX;
        /** Whether to flip the image horizontally (true when flying left). */
        public final boolean flipX;

        public ProjectileSprite(BufferedImage image, double startWorldX, double targetWorldX,
                                double worldY, double speedPxPerTick) {
            this.image       = image;
            this.worldX      = startWorldX;
            this.targetWorldX = targetWorldX;
            this.worldY      = worldY;
            this.flipX       = targetWorldX < startWorldX;
            this.velocityX   = flipX ? -speedPxPerTick : speedPxPerTick;
        }

        /**
         * Advances one render tick.
         * @return false once the projectile has reached or passed the target
         */
        public boolean tick() {
            worldX += velocityX;
            if (flipX) return worldX > targetWorldX;
            else       return worldX < targetWorldX;
        }
    }

    /** Advances every ProjectileSprite in the list and removes those that have arrived. */
    public static void updateProjectiles(List<ProjectileSprite> projectiles) {
        projectiles.removeIf(p -> !p.tick());
    }

    /**
     * Draws all in-flight projectiles.
     * The image is centered horizontally on worldX and vertically on worldY,
     * projected onto the panel's isometric ground plane.
     */
    public static void drawProjectiles(Graphics2D g2, List<ProjectileSprite> projectiles,
                                       int panelW, int groundBase, double isoScale) {
        for (ProjectileSprite p : projectiles) {
            int w = p.image.getWidth();
            int h = p.image.getHeight();
            int sx = (int)(panelW / 2.0 + p.worldX) - w / 2;
            int sy = (int)(groundBase - p.worldY * isoScale) - h / 2;
            if (p.flipX) g2.drawImage(p.image, sx + w, sy, -w, h, null);
            else         g2.drawImage(p.image, sx,     sy,  w, h, null);
        }
    }

    // ── Text utilities ────────────────────────────────────────────────────────

    /** Splits text into lines that each fit within maxWidth pixels under the given FontMetrics. */
    public static List<String> wrapText(String text, FontMetrics fm, int maxWidth) {
        List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.isEmpty() ? word : current + " " + word;
            if (fm.stringWidth(test) <= maxWidth) {
                current = new StringBuilder(test);
            } else {
                if (!current.isEmpty()) lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    /** Returns "1st", "2nd", "3rd", "4th", etc. — used on victory damage-ranking screens. */
    public static String ordinal(int rank) {
        return switch (rank) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> rank + "th";
        };
    }
}