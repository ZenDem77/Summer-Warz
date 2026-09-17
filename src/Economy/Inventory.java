package Economy;

import Entities.Artifacts.Artifact;
import Entities.Character;
import Entities.Items.*;
import Entities.Weapons.Weapon;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Inventory — the shared account-wide item pool.
 *
 * Holds all weapons, artifacts, ascension crystals, and misc items
 * owned by the player. Characters equip FROM this pool — items don't
 * live on the character, they live here and reference which character
 * they're currently equipped to (or null if unequipped).
 *
 * ── Capacity ─────────────────────────────────────────────────────────
 * MAX_CAPACITY is the total slot limit. Each weapon, artifact, and misc
 * item occupies 1 slot. All crystals for the same character share 1 slot
 * (the stack), so crystal farming never bloats the count.
 *
 * To change the cap: edit MAX_CAPACITY — it's the only number to touch.
 *
 * ── Equip contract ───────────────────────────────────────────────────
 * Equipping and unequipping goes through Inventory methods (not directly
 * on OwnedWeapon/OwnedArtifact) so the inventory can enforce the one-
 * character-per-item rule and update stats correctly.
 */
public class Inventory {

    // ── Easy-to-change capacity cap ───────────────────────────────────────────
    public static final int MAX_CAPACITY = 500;

    // ── Storage ───────────────────────────────────────────────────────────────
    private final List<OwnedWeapon>               weapons    = new ArrayList<>();
    private final List<OwnedArtifact>             artifacts  = new ArrayList<>();
    /** One stack per character name — crystals for the same character share a slot. */
    private final Map<String, AscensionCrystal>   crystals   = new LinkedHashMap<>();
    private final List<MiscItem>                  miscItems  = new ArrayList<>();

    // ── Capacity ──────────────────────────────────────────────────────────────

    /**
     * Current slot usage — weapons + artifacts + misc + 1 per crystal stack.
     * Crystal stacks are fixed-size regardless of quantity.
     */
    public int getUsedSlots() {
        return weapons.size() + artifacts.size() + miscItems.size() + crystals.size();
    }

    public int  getRemainingSlots() { return MAX_CAPACITY - getUsedSlots(); }
    public boolean isFull()         { return getUsedSlots() >= MAX_CAPACITY; }

    // ── Add items ─────────────────────────────────────────────────────────────

    /**
     * Adds a new unique weapon to the pool.
     * @throws IllegalStateException if the inventory is full.
     */
    public OwnedWeapon addWeapon(Weapon weapon) {
        checkCapacity();
        OwnedWeapon owned = new OwnedWeapon(weapon);
        weapons.add(owned);
        return owned;
    }

    /**
     * Adds a new unique artifact to the pool.
     * @throws IllegalStateException if the inventory is full.
     */
    public OwnedArtifact addArtifact(Artifact artifact) {
        checkCapacity();
        OwnedArtifact owned = new OwnedArtifact(artifact);
        artifacts.add(owned);
        return owned;
    }

    /**
     * Adds ascension crystals for the given character.
     * Creates a new stack if this is the first crystal for that character
     * (occupies 1 new slot); otherwise adds to the existing stack.
     * A new stack still requires a free slot.
     */
    public void addCrystals(String characterName, int amount) {
        if (!crystals.containsKey(characterName)) {
            checkCapacity();   // first crystal for this character needs a slot
            crystals.put(characterName, new AscensionCrystal(characterName, amount));
        } else {
            crystals.get(characterName).add(amount);
        }
    }

    /**
     * Adds a misc item to the pool.
     * @throws IllegalStateException if the inventory is full.
     */
    public MiscItem addMiscItem(String name, String description) {
        checkCapacity();
        MiscItem item = new MiscItem(name, description);
        miscItems.add(item);
        return item;
    }

    // ── Remove items ──────────────────────────────────────────────────────────

    /**
     * Removes a weapon from the pool. Unequips it first if equipped.
     */
    public boolean removeWeapon(OwnedWeapon weapon) {
        if (weapon.isEquipped()) weapon.unequip();
        return weapons.remove(weapon);
    }

    /**
     * Removes an artifact from the pool. Unequips it first if equipped.
     */
    public boolean removeArtifact(OwnedArtifact artifact) {
        if (artifact.isEquipped()) artifact.unequip();
        return artifacts.remove(artifact);
    }

    /**
     * Removes a misc item from the pool.
     */
    public boolean removeMiscItem(MiscItem item) {
        return miscItems.remove(item);
    }

    // ── Equip / unequip weapons ───────────────────────────────────────────────

    /**
     * Equips the given weapon to the given character.
     * If the character already has a weapon equipped, that weapon is
     * automatically unequipped and returned to the pool first.
     *
     * @throws IllegalStateException if the weapon is already equipped by someone else.
     */
    public void equipWeapon(OwnedWeapon weapon, Character character) {
        if (weapon.isEquipped() && weapon.getEquippedBy() != character)
            throw new IllegalStateException(
                    weapon.getName() + " is equipped by " + weapon.getEquippedBy().getName());

        // Unequip the character's current weapon first
        getEquippedWeapon(character).ifPresent(OwnedWeapon::unequip);
        weapon.equipTo(character);
    }

    /**
     * Unequips whatever weapon the character is currently holding.
     * No-op if the character has no weapon.
     */
    public void unequipWeapon(Character character) {
        getEquippedWeapon(character).ifPresent(OwnedWeapon::unequip);
    }

    /** Returns the weapon currently equipped by this character, if any. */
    public Optional<OwnedWeapon> getEquippedWeapon(Character character) {
        return weapons.stream()
                .filter(w -> w.isEquipped() && w.getEquippedBy() == character)
                .findFirst();
    }

    // ── Equip / unequip artifacts ─────────────────────────────────────────────

    /**
     * Equips the given artifact to the character in the specified slot.
     * Any artifact already in that slot is unequipped first.
     *
     * @throws IllegalStateException if the artifact is equipped by someone else,
     *                               or if the slot is not yet unlocked.
     */
    public void equipArtifact(OwnedArtifact artifact, Character character, int slot) {
        if (artifact.isEquipped() && artifact.getEquippedBy() != character)
            throw new IllegalStateException(
                    "Artifact is already equipped by " + artifact.getEquippedBy().getName());

        getEquippedArtifactInSlot(character, slot).ifPresent(OwnedArtifact::unequip);
        artifact.equipTo(character, slot);
    }

    /** Unequips the artifact in the specified slot for this character. */
    public void unequipArtifact(Character character, int slot) {
        getEquippedArtifactInSlot(character, slot).ifPresent(OwnedArtifact::unequip);
    }

    /** Returns the artifact in the specified slot for this character, if any. */
    public Optional<OwnedArtifact> getEquippedArtifactInSlot(Character character, int slot) {
        return artifacts.stream()
                .filter(a -> a.isEquipped()
                        && a.getEquippedBy() == character
                        && a.getSlotIndex() == slot)
                .findFirst();
    }

    /** Returns all artifacts currently equipped by this character. */
    public List<OwnedArtifact> getEquippedArtifacts(Character character) {
        return artifacts.stream()
                .filter(a -> a.isEquipped() && a.getEquippedBy() == character)
                .collect(Collectors.toList());
    }

    // ── Crystal queries ───────────────────────────────────────────────────────

    /** Returns the crystal stack for the given character, or empty. */
    public Optional<AscensionCrystal> getCrystals(String characterName) {
        return Optional.ofNullable(crystals.get(characterName));
    }

    /** Returns the quantity of crystals owned for the given character (0 if none). */
    public int getCrystalCount(String characterName) {
        return crystals.containsKey(characterName)
                ? crystals.get(characterName).getQuantity() : 0;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public List<OwnedWeapon>   getWeapons()   { return Collections.unmodifiableList(weapons); }
    public List<OwnedArtifact> getArtifacts() { return Collections.unmodifiableList(artifacts); }
    public List<MiscItem>      getMiscItems() { return Collections.unmodifiableList(miscItems); }

    public Collection<AscensionCrystal> getAllCrystals() {
        return Collections.unmodifiableCollection(crystals.values());
    }

    /** All unequipped weapons (available to equip). */
    public List<OwnedWeapon> getUnequippedWeapons() {
        return weapons.stream().filter(w -> !w.isEquipped()).collect(Collectors.toList());
    }

    /** All unequipped artifacts (available to equip). */
    public List<OwnedArtifact> getUnequippedArtifacts() {
        return artifacts.stream().filter(a -> !a.isEquipped()).collect(Collectors.toList());
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void checkCapacity() {
        if (isFull()) throw new IllegalStateException(
                "Inventory is full (" + MAX_CAPACITY + " slots). Remove items to make space.");
    }

    @Override
    public String toString() {
        return "Inventory[" + getUsedSlots() + "/" + MAX_CAPACITY + " slots"
                + " | weapons=" + weapons.size()
                + " artifacts=" + artifacts.size()
                + " crystalStacks=" + crystals.size()
                + " misc=" + miscItems.size() + "]";
    }
}