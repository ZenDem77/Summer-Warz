package Entities.Items;

/**
 * ItemType — the four inventory categories.
 *
 * WEAPON    — unique weapon instances, one equipped per character
 * ARTIFACT  — unique artifact instances, up to 4 equipped per character
 * ASCENSION — stackable character-specific crystals for ascending a character
 * MISC      — catch-all for future items (quest items, event items, materials)
 */
public enum ItemType {
    WEAPON,
    ARTIFACT,
    ASCENSION,
    MISC
}