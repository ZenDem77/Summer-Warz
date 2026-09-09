package Entities.Items;

import Entities.Artifacts.Artifact;
import Entities.Character;

/**
 * OwnedArtifact — a unique instance of an artifact in the shared inventory pool.
 *
 * Like OwnedWeapon, each artifact is a distinct object regardless of its stats,
 * and can only be equipped by one character at a time.
 *
 * slotIndex is the artifact slot on the character (0–3, matching Character's
 * isSlotUnlocked / getSlotUnlockLevel system). -1 when unequipped.
 */
public class OwnedArtifact extends InventoryItem {

    private final Artifact  artifact;
    private       Character equippedBy;    // null = unequipped
    private       int       slotIndex;     // 0-3 when equipped, -1 when not

    public OwnedArtifact(Artifact artifact) {
        super(ItemType.ARTIFACT);
        this.artifact   = artifact;
        this.equippedBy = null;
        this.slotIndex  = -1;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Artifact  getArtifact()   { return artifact; }
    public Character getEquippedBy() { return equippedBy; }
    public int       getSlotIndex()  { return slotIndex; }
    public boolean   isEquipped()    { return equippedBy != null; }

    // ── Equip / unequip ───────────────────────────────────────────────────────

    /**
     * Equips this artifact to the given character in the given slot.
     * Caller is responsible for ensuring:
     *   - the slot is unlocked on the character
     *   - the slot is not already occupied (or the old artifact has been unequipped)
     *   - this artifact is not already equipped by another character
     */
    public void equipTo(Character character, int slot) {
        if (isEquipped()) throw new IllegalStateException(
                "Artifact already equipped by " + equippedBy.getName());
        if (!character.isSlotUnlocked(slot)) throw new IllegalStateException(
                "Slot " + slot + " is not unlocked for " + character.getName()
                        + " (requires Lv" + character.getSlotUnlockLevel(slot) + ")");

        equippedBy = character;
        slotIndex  = slot;
        character.equipArtifact(slot, artifact);   // correct param order: (int slot, Artifact)
    }

    public void unequip() {
        if (equippedBy == null) return;
        equippedBy.unequipArtifact(slotIndex);
        equippedBy = null;
        slotIndex  = -1;
    }

    // ── InventoryItem ─────────────────────────────────────────────────────────

    @Override
    public String getName() {
        // Artifact has no getName() — display its substats as the identifier
        return artifact.toString();
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        artifact.getSubstats().forEach(s -> sb.append(s.getDisplayString()).append("  "));
        if (isEquipped()) sb.append("[Equipped: ").append(equippedBy.getName())
                .append(" slot ").append(slotIndex).append("]");
        else sb.append("[Unequipped]");
        return sb.toString().trim();
    }
}