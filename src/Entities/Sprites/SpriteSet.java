package Entities.Sprites;

/**
 * SpriteSet — the 5 sprite file paths for one character or enemy:
 * idle, run, attack (cast/swing pose), dead, and projectile.
 *
 * Any path can be left as null or "" — SpriteLoader will return null for
 * that slot, and the battle panels will automatically fall back to the
 * generated placeholder sprite instead of crashing.
 *
 * projectilePath is optional — null means this entity has no traveling
 * projectile effect. When set, the battle panels launch that image toward
 * the opponent every time this entity's attack tick fires.
 *
 * You don't need to construct this directly — see SpritePaths.java for the
 * one place you actually edit file paths.
 */
public record SpriteSet(String idlePath, String runPath, String attackPath,
                        String deadPath, String projectilePath) {

    /** Convenience constructor with no projectile (covers all existing characters/enemies). */
    public SpriteSet(String idlePath, String runPath, String attackPath, String deadPath) {
        this(idlePath, runPath, attackPath, deadPath, null);
    }

    /** A SpriteSet with no paths set — signals "use the placeholder sprite for everything". */
    public static final SpriteSet NONE = new SpriteSet(null, null, null, null, null);
}