package Entities.Items;

/**
 * MiscItem — catch-all inventory item for anything that doesn't fit
 * the Weapon, Artifact, or Ascension categories.
 *
 * Intended for future additions: quest items, event items, generic
 * materials, cosmetics, etc. Each MiscItem is a unique instance
 * with its own UUID, same as weapons and artifacts.
 *
 * To add a new misc item type in the future, just subclass MiscItem
 * (or instantiate it directly with a name/description string pair).
 */
public class MiscItem extends InventoryItem {

    private final String name;
    private final String description;

    public MiscItem(String name, String description) {
        super(ItemType.MISC);
        this.name        = name;
        this.description = description;
    }

    @Override public String getName()        { return name; }
    @Override public String getDescription() { return description; }
}