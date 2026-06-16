package Economy;

import java.util.Map;

/**
 * FloorRewardTable — Gold + Elixir granted to the player for clearing a floor.
 *
 * Deliberately kept separate from Floor/FloorMode so you can tune rewards
 * without ever touching enemy definitions, and vice versa.
 *
 * ── TO CUSTOMIZE ─────────────────────────────────────────────────────────
 * Just edit the REWARDS map below — add, remove, or change entries freely.
 * Floors with no entry default to Reward(0, 0) — see getReward().
 *
 * To extend to floors 11+ later, just add more Map.entry(...) lines here;
 * nothing else in FloorMode needs to change.
 */
public class FloorRewardTable {

    /** Immutable (gold, elixir) pair granted for clearing one floor. */
    public record Reward(int gold, int elixir) {}

    private static final Map<Integer, Reward> REWARDS = Map.ofEntries(
            Map.entry(1,  new Reward(50,  10)),
            Map.entry(2,  new Reward(60,  10)),
            Map.entry(3,  new Reward(70,  15)),
            Map.entry(4,  new Reward(80,  15)),
            Map.entry(5,  new Reward(150, 40)),   // ★ boss floor — bigger reward
            Map.entry(6,  new Reward(90,  20)),
            Map.entry(7,  new Reward(100, 20)),
            Map.entry(8,  new Reward(110, 25)),
            Map.entry(9,  new Reward(120, 25)),
            Map.entry(10, new Reward(250, 60))    // ★ boss floor — bigger reward
    );

    /** Returns the reward for the given floor number, or Reward(0, 0) if undefined. */
    public static Reward getReward(int floorNumber) {
        return REWARDS.getOrDefault(floorNumber, new Reward(0, 0));
    }
}