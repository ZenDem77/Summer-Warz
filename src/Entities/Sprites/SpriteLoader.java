package Entities.Sprites;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * SpriteLoader — loads sprite images from disk, scales them up with
 * NEAREST-NEIGHBOR interpolation (not the smooth/bilinear scaling Java uses
 * by default, which is what keeps pixel art crisp instead of blurry), then
 * pads the result onto a fixed-size canvas matching the game's sprite box —
 * bottom-center anchored, just like a placeholder sprite always has been.
 *
 * This last padding step is what lets every battle panel keep using its
 * EXISTING positioning math (shadow, flash overlay, name tag, etc. are all
 * built around one fixed sprite box size) completely unchanged — loaded art
 * just becomes a drop-in replacement for a placeholder image, regardless of
 * how tightly each individual pose's source art was cropped.
 *
 * Each sprite is only loaded, scaled, and padded once — cached after that.
 *
 * If a file path is null, blank, or can't be read, load() returns null so
 * callers can fall back to a placeholder sprite instead of crashing.
 *
 * ── TO CUSTOMIZE ─────────────────────────────────────────────────────────
 * UPSCALE_FACTOR is the one number you'd ever want to tune — raise it if
 * sprites look too small within their box, lower it if they look too big
 * or get clipped. Source art is intentionally exported at tiny true
 * pixel-art resolution; this is what blows it back up to game size.
 */
public class SpriteLoader {

    /** How much to scale up from native sprite-file resolution before padding into the box. */
    public static final int UPSCALE_FACTOR = 2;

    private static final Map<String, BufferedImage> cache = new HashMap<>();

    /**
     * Loads an image from the given path, scales it up by UPSCALE_FACTOR
     * (nearest-neighbor), then pads it onto a boxWidth × boxHeight canvas,
     * bottom-center anchored.
     *
     * Supports two kinds of paths, tried in this order:
     *   1. Classpath resource (e.g. "/characters/kaizen/kaizen_idle.png") —
     *      works if your sprites live under a folder marked as a Resources
     *      Root in IntelliJ, same as how background images are loaded
     *      elsewhere in this project. Must start with "/".
     *   2. Literal filesystem path (e.g. "C:/Users/Owner/.../kaizen_idle.png") —
     *      works regardless of project setup, used as a fallback.
     *
     * @param filePath  classpath resource path (starting with "/") or absolute filesystem path
     * @param boxWidth  the game's fixed sprite box width (e.g. SPRITE_W)
     * @param boxHeight the game's fixed sprite box height (e.g. SPRITE_H)
     * @return a boxWidth × boxHeight image ready to drop straight into spriteCache,
     *         or null if filePath is null/blank or the image can't be found either way
     */
    public static BufferedImage load(String filePath, int boxWidth, int boxHeight) {
        if (filePath == null || filePath.isBlank()) return null;

        String cacheKey = filePath + "@" + boxWidth + "x" + boxHeight;
        if (cache.containsKey(cacheKey)) return cache.get(cacheKey);

        BufferedImage source = null;

        // Try 1: classpath resource (matches how background images are loaded elsewhere)
        if (filePath.startsWith("/")) {
            java.net.URL resource = SpriteLoader.class.getResource(filePath);
            if (resource != null) {
                try {
                    source = ImageIO.read(resource);
                } catch (IOException e) {
                    System.out.println("[SpriteLoader] Found resource but couldn't read it: " + filePath + " (" + e.getMessage() + ")");
                }
            }
        }

        // Try 2: literal filesystem path
        if (source == null) {
            try {
                File file = new File(filePath);
                if (file.exists()) {
                    source = ImageIO.read(file);
                } else {
                    System.out.println("[SpriteLoader] Sprite not found as classpath resource or filesystem path: " + filePath);
                }
            } catch (IOException e) {
                System.out.println("[SpriteLoader] Failed to load sprite: " + filePath + " (" + e.getMessage() + ")");
            }
        }

        BufferedImage result = null;
        if (source != null) {
            int upW = source.getWidth()  * UPSCALE_FACTOR;
            int upH = source.getHeight() * UPSCALE_FACTOR;
            BufferedImage upscaled = scaleNearestNeighbor(source, upW, upH);
            result = padIntoBox(upscaled, boxWidth, boxHeight);
        }

        cache.put(cacheKey, result);
        return result;
    }

    /**
     * Scales an image using nearest-neighbor interpolation — this is what
     * keeps pixel art looking crisp and blocky instead of getting smoothed
     * into a blur by Java's default scaling.
     */
    private static BufferedImage scaleNearestNeighbor(BufferedImage source, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2.drawImage(source, 0, 0, width, height, null);
        g2.dispose();
        return scaled;
    }

    /**
     * Pastes an image onto a transparent boxWidth × boxHeight canvas,
     * horizontally centered and bottom-aligned. If the image is larger than
     * the box in either dimension, it's centered/bottom-aligned without
     * further scaling (it will simply extend past the box edges — raise
     * UPSCALE_FACTOR down if this happens and isn't what you want).
     */
    private static BufferedImage padIntoBox(BufferedImage img, int boxWidth, int boxHeight) {
        BufferedImage canvas = new BufferedImage(boxWidth, boxHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        int x = (boxWidth - img.getWidth()) / 2;
        int y = boxHeight - img.getHeight();   // bottom-aligned
        g2.drawImage(img, x, y, null);
        g2.dispose();
        return canvas;
    }

    /** Clears the cache — useful if you want to reload sprites without restarting the game. */
    public static void clearCache() { cache.clear(); }
}
