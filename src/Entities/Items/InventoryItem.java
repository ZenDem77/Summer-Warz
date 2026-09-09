package Entities.Items;

import java.util.UUID;

/**
 * InventoryItem — abstract base for every item in the shared inventory pool.
 *
 * Every item has a universally unique ID (UUID) so that two weapons of the
 * same type and level are still distinct objects — important for the equip
 * system where each unique instance can only be held by one character at a time.
 */
public abstract class InventoryItem {

    private final String id;          // UUID — unique per instance
    private final ItemType itemType;

    protected InventoryItem(ItemType itemType) {
        this.id       = UUID.randomUUID().toString();
        this.itemType = itemType;
    }

    public String   getId()       { return id; }
    public ItemType getItemType() { return itemType; }

    /** Display name shown in the inventory UI. */
    public abstract String getName();

    /** Short description shown in the inventory UI. */
    public abstract String getDescription();

    @Override
    public String toString() {
        return "[" + itemType + "] " + getName() + " (" + id.substring(0, 8) + ")";
    }
}