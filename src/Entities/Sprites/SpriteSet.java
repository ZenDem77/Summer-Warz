package Entities.Sprites;

/**
 * SpriteSet — the 4 sprite file paths for one character or enemy:
 * idle, run, attack, and dead.
 *
 * Any path can be left as null or "" — SpriteLoader will return null for
 * that slot, and the battle panels will automatically fall back to the
 * generated placeholder sprite instead of crashing.
 *
 * You don't need to construct this directly — see SpritePaths.java for the
 * one place you actually edit file paths.
 */
public record SpriteSet(String idlePath, String runPath, String attackPath, String deadPath) {

    /** A SpriteSet with no paths set — signals "use the placeholder sprite for everything". */
    public static final SpriteSet NONE = new SpriteSet(null, null, null, null);
}