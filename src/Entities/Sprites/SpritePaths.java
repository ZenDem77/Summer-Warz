package Entities.Sprites;

/**
 * SpritePaths — THE place to set sprite file paths for every character and
 * enemy in the game. This is the only file you should need to touch when
 * adding new sprite art.
 *
 * ── HOW TO USE ───────────────────────────────────────────────────────────
 * Paste the full file path to each PNG between the quotes below.
 * Leave a line as "" (empty string) if you don't have that sprite yet —
 * the game will automatically draw a placeholder shape instead, so nothing
 * breaks while art is still in progress.
 *
 * Paths can be absolute (e.g. "C:/Users/Owner/IdeaProjects/Summer Warz/res/kaizen_idle.png")
 * or, if you put sprites in a resources folder marked as a Resources Root
 * in IntelliJ, a classpath-relative path may work too — but plain absolute
 * paths are the simplest to get right, so start there.
 *
 * Required sprite per character/enemy: idle, run, attack, dead.
 * (See Entities/SpriteSet.java for what each slot means.)
 */
public class SpritePaths {

    // ── Kaizen ────────────────────────────────────────────────────────────────
    public static final String KAIZEN_IDLE   = "/characters/kaizen/kaizen_idle.png";
    public static final String KAIZEN_RUN    = "/characters/kaizen/kaizen_run.png";
    public static final String KAIZEN_ATTACK = "/characters/kaizen/kaizen_attack.png";
    public static final String KAIZEN_DEAD   = "/characters/kaizen/kaizen_dead.png";

    // ── Zed ───────────────────────────────────────────────────────────────────
    public static final String ZED_IDLE   = "";
    public static final String ZED_RUN    = "";
    public static final String ZED_ATTACK = "";
    public static final String ZED_DEAD   = "";

    // ── Zayir ─────────────────────────────────────────────────────────────────
    public static final String ZAYIR_IDLE   = "";
    public static final String ZAYIR_RUN    = "";
    public static final String ZAYIR_ATTACK = "";
    public static final String ZAYIR_DEAD   = "";

    // ── Add more characters/enemies here following the same pattern ──────────
    // Example:
    // public static final String PHAINON_IDLE   = "";
    // public static final String PHAINON_RUN    = "";
    // public static final String PHAINON_ATTACK = "";
    // public static final String PHAINON_DEAD   = "";
}