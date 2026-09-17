package Economy;

import java.util.EnumMap;
import java.util.Map;

/**
 * Wallet — holds the player's balance for every Currency.
 *
 * This is meant to be a single, player-wide object — create one when the
 * player starts the game (or loads a save) and pass it to whichever
 * subsystem needs it: shop, FloorMode (for floor rewards), LevelingService
 * (for character/weapon leveling costs), etc.
 *
 * Usage:
 *   Wallet wallet = new Wallet();
 *   wallet.add(Currency.GOLD, 100);
 *   if (wallet.spend(Currency.ELIXIR, 50)) { ... }
 */
public class Wallet {

    private final Map<Currency, Integer> balances = new EnumMap<>(Currency.class);

    public Wallet() {
        for (Currency c : Currency.values()) balances.put(c, 0);
    }

    /** Starts the wallet with specific opening balances (e.g. for testing or a fresh save). */
    public Wallet(int startingGold, int startingElixir) {
        this();
        balances.put(Currency.GOLD, startingGold);
        balances.put(Currency.ELIXIR, startingElixir);
    }

    // ── Balance queries ──────────────────────────────────────────────────────

    public int getBalance(Currency currency) { return balances.getOrDefault(currency, 0); }
    public int getGold()   { return getBalance(Currency.GOLD); }
    public int getElixir() { return getBalance(Currency.ELIXIR); }

    public boolean hasEnough(Currency currency, int amount) {
        return getBalance(currency) >= amount;
    }

    // ── Mutation ──────────────────────────────────────────────────────────────

    /** Adds the given amount to the balance. Used for rewards, shop refunds, etc. */
    public void add(Currency currency, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Cannot add a negative amount.");
        balances.merge(currency, amount, Integer::sum);
    }

    /**
     * Attempts to spend the given amount.
     * If the balance is insufficient, nothing is deducted and false is returned.
     *
     * @return true if the spend succeeded, false if there wasn't enough balance
     */
    public boolean spend(Currency currency, int amount) {
        if (amount < 0) throw new IllegalArgumentException("Cannot spend a negative amount.");
        if (!hasEnough(currency, amount)) return false;
        balances.merge(currency, -amount, Integer::sum);
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Wallet [");
        for (Currency c : Currency.values()) {
            sb.append(c).append(": ").append(getBalance(c)).append("  ");
        }
        return sb.append("]").toString();
    }
}