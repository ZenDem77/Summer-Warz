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
 * Paths can be absolute (e.g. "C:/Users/Owner/IdeaProjects/Summer Warz/res/phainon_idle.png")
 * or, if you put sprites in a resources folder marked as a Resources Root
 * in IntelliJ, a classpath-relative path may work too — but plain absolute
 * paths are the simplest to get right, so start there.
 *
 * Required sprite per character/enemy: idle, run, attack, dead.
 * (See Entities/SpriteSet.java for what each slot means.)
 */
public class SpritePaths {

    // ── Xyniz ────────────────────────────────────────────────────────────────
    public static final String XYNIZ_IDLE = "/characters/xyniz/Xyniz_Idle.png";
    public static final String XYNIZ_RUN = "/characters/xyniz/Xyniz_Dash.png";
    public static final String XYNIZ_ATTACK = "/characters/xyniz/Xyniz_Attack.png";
    public static final String XYNIZ_DEAD = "/characters/xyniz/Xyniz_Dead.png";
    public static final String XYNIZ_PROJECTILE = "/characters/xyniz/Xyniz_Attack_2.png";

    // ── Kindle ───────────────────────────────────────────────────────────────────
    public static final String KINDLE_IDLE = "/characters/kindle/Kindle_Idle.png";
    public static final String KINDLE_RUN = "/characters/kindle/Kindle_Run.png";
    public static final String KINDLE_ATTACK = "/characters/kindle/Kindle_Attack.png";
    public static final String KINDLE_DEAD = "/characters/kindle/Kindle_Dead.png";

    // ── Zayir ─────────────────────────────────────────────────────────────────
    public static final String ZAYIR_IDLE   = "";
    public static final String ZAYIR_RUN    = "";
    public static final String ZAYIR_ATTACK = "";
    public static final String ZAYIR_DEAD   = "";

    // ── Zenzenkoi ─────────────────────────────────────────────────────────────
    public static final String ZENZENKOI_IDLE   = "";
    public static final String ZENZENKOI_RUN    = "";
    public static final String ZENZENKOI_ATTACK = "";
    public static final String ZENZENKOI_DEAD   = "";

    // ── Kouzen ────────────────────────────────────────────────────────────────
    public static final String KOUZEN_IDLE   = "";
    public static final String KOUZEN_RUN    = "";
    public static final String KOUZEN_ATTACK = "";
    public static final String KOUZEN_DEAD   = "";

    // ──────────────────────────────────────────────────────────────────────────
    // ── BOSSES ──
    // ──────────────────────────────────────────────────────────────────────────

    // ── Fiend ───────────────────────────────────────────────────────────────
     public static final String FIEND_IDLE = "/bosses/phainon/phainon_idle.png";
     public static final String FIEND_RUN = "/bosses/phainon/phainon_run.png";
     public static final String FIEND_ATTACK = "/bosses/phainon/phainon_attack.png";
     public static final String FIEND_DEAD = "/bosses/phainon/phainon_dead.png";
}