package Economy;

import Entities.Character;
import Entities.Weapons.Weapon;

/**
 * LevelingService — ties Wallet + the cost tables together with
 * Character/Weapon leveling, so the rest of the game doesn't need to
 * manually check balances and spend currency every time.
 *
 * Both methods charge Elixir for the ATTEMPT, not for guaranteed success.
 * A weapon upgrade can still fail its own RNG roll (see Weapon.attemptUpgrade())
 * even after the Elixir has been spent — that's intentional, matching how
 * these systems usually work (you pay for the chance, not the outcome).
 *
 * Usage:
 *   LevelingService.LevelUpResult result = LevelingService.levelUpCharacter(zed, wallet);
 *   if (result == LevelingService.LevelUpResult.SUCCESS) { ... }
 */
public class LevelingService {

    public enum LevelUpResult { SUCCESS, INSUFFICIENT_ELIXIR, MAX_LEVEL_REACHED }

    /**
     * Attempts to level up a character by exactly one level.
     * Charges CharacterLevelCost.getCost(character.getLevel()) Elixir from the wallet.
     *
     * @return SUCCESS if the character leveled up and Elixir was charged,
     *         INSUFFICIENT_ELIXIR if the wallet didn't have enough
     *         (nothing is charged and the character does not level up)
     */
    public static LevelUpResult levelUpCharacter(Character character, Wallet wallet) {
        int cost = CharacterLevelCost.getCost(character.getLevel());
        if (!wallet.spend(Currency.ELIXIR, cost)) return LevelUpResult.INSUFFICIENT_ELIXIR;
        character.gainLevel();
        return LevelUpResult.SUCCESS;
    }

    /**
     * Attempts to upgrade a weapon by one level (subject to the weapon's own
     * success-rate roll). Charges WeaponUpgradeCost.getCost(weapon.getWeaponLevel())
     * Elixir regardless of whether the roll itself succeeds or fails.
     *
     * If you need to know whether the upgrade roll succeeded (vs just whether
     * the Elixir was charged), call weapon.attemptUpgrade()'s return value
     * directly instead — or check weapon.getWeaponLevel() before/after this call.
     *
     * @return SUCCESS if Elixir was charged and the upgrade attempt was made,
     *         INSUFFICIENT_ELIXIR if the wallet didn't have enough,
     *         MAX_LEVEL_REACHED if the weapon is already at +20 (nothing charged)
     */
    public static LevelUpResult upgradeWeapon(Weapon weapon, Wallet wallet) {
        if (!weapon.canUpgrade()) return LevelUpResult.MAX_LEVEL_REACHED;
        int cost = WeaponUpgradeCost.getCost(weapon.getWeaponLevel());
        if (!wallet.spend(Currency.ELIXIR, cost)) return LevelUpResult.INSUFFICIENT_ELIXIR;
        weapon.attemptUpgrade();
        return LevelUpResult.SUCCESS;
    }
}