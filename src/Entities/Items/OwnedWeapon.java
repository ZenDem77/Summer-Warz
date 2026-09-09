package Entities.Items;

import Entities.Character;
import Entities.Weapons.Weapon;

/**
 * OwnedWeapon — a unique instance of a weapon in the shared inventory pool.
 *
 * Delegates equip/unequip to Weapon's own equip() and unequip() methods
 * (which handle applyWeaponStats / removeWeaponStats internally).
 * OwnedWeapon's equippedBy tracks which character holds this instance for
 * inventory queries — kept in sync with Weapon's internal equippedOn.
 */
public class OwnedWeapon extends InventoryItem {

    private final Weapon    weapon;
    private       Character equippedBy;

    public OwnedWeapon(Weapon weapon) {
        super(ItemType.WEAPON);
        this.weapon     = weapon;
        this.equippedBy = null;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public Weapon    getWeapon()     { return weapon; }
    public Character getEquippedBy() { return equippedBy; }
    public boolean   isEquipped()    { return equippedBy != null; }

    // ── Equip / unequip ───────────────────────────────────────────────────────

    public void equipTo(Character character) {
        if (isEquipped()) throw new IllegalStateException(
                weapon.getName() + " is already equipped by " + equippedBy.getName());
        weapon.equip(character);   // Weapon.equip() calls character.applyWeaponStats() internally
        equippedBy = character;
    }

    public void unequip() {
        if (equippedBy == null) return;
        weapon.unequip();          // Weapon.unequip() calls equippedOn.removeWeaponStats() internally
        equippedBy = null;
    }

    // ── InventoryItem ─────────────────────────────────────────────────────────

    @Override
    public String getName() {
        return weapon.getName() + " +" + weapon.getWeaponLevel();
    }

    @Override
    public String getDescription() {
        return "ATK: " + weapon.getCurrentAtk()
                + (weapon.hasSecondaryStat()
                ? "  |  " + weapon.getSecondaryStatType()
                + ": " + weapon.getCurrentSecondaryValue()
                : "")
                + (isEquipped() ? "  [Equipped: " + equippedBy.getName() + "]" : "  [Unequipped]");
    }
}