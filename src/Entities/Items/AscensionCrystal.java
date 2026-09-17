package Entities.Items;

/**
 * AscensionCrystal — stackable ascension material tied to a specific character.
 *
 * Each character has their own crystal type (e.g. "Xyniz Crystal", "Kindle Crystal").
 * Crystals are NOT unique instances — they stack freely. The inventory holds one
 * AscensionCrystal entry per character name, tracking the total quantity owned.
 *
 * Phase costs (crystals required to ascend):
 *   Phase 1 → 4    (unlocks lv 11-20)
 *   Phase 2 → 8    (unlocks lv 21-30)
 *   Phase 3 → 16   (unlocks lv 31-40)
 *   Phase 4 → 32   (unlocks lv 41-50)
 *   Phase 5 → 64   (unlocks lv 51-60)
 */
public class AscensionCrystal extends InventoryItem {

    // ── Phase cost table ──────────────────────────────────────────────────────
    private static final int[] PHASE_COSTS = { 0, 4, 8, 16, 32, 64 };
    public  static final int   MAX_PHASE   = 5;

    /** Crystal cost to ascend to the given phase (1–5). */
    public static int costForPhase(int targetPhase) {
        if (targetPhase < 1 || targetPhase > MAX_PHASE)
            throw new IllegalArgumentException("Invalid ascension phase: " + targetPhase);
        return PHASE_COSTS[targetPhase];
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    /** Name of the character this crystal belongs to (e.g. "Xyniz"). */
    private final String characterName;
    private       int    quantity;

    public AscensionCrystal(String characterName, int quantity) {
        super(ItemType.ASCENSION);
        if (quantity < 0) throw new IllegalArgumentException("Quantity cannot be negative.");
        this.characterName = characterName;
        this.quantity      = quantity;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String getCharacterName() { return characterName; }
    public int    getQuantity()      { return quantity; }
    public boolean isEmpty()         { return quantity <= 0; }

    // ── Mutation ──────────────────────────────────────────────────────────────

    /** Adds crystals to this stack (e.g. from farming rewards). */
    public void add(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Add amount must be positive.");
        quantity += amount;
    }

    /**
     * Deducts crystals from this stack.
     * @throws IllegalStateException if there are not enough crystals.
     */
    public void deduct(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deduct amount must be positive.");
        if (amount > quantity) throw new IllegalStateException(
                "Not enough " + getName() + ": need " + amount + ", have " + quantity);
        quantity -= amount;
    }

    public boolean hasEnough(int amount) { return quantity >= amount; }

    // ── InventoryItem ─────────────────────────────────────────────────────────

    @Override
    public String getName() { return characterName + " Crystal"; }

    @Override
    public String getDescription() {
        return "Ascension material for " + characterName
                + ".  Quantity: " + quantity
                + "  |  Phase costs: 4 / 8 / 16 / 32 / 64";
    }
}