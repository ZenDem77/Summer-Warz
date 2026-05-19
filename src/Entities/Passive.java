package Entities;

import Combat.IBattle;
import java.util.EnumSet;
import java.util.Set;

public interface Passive {

    // ── Identity ──────────────────────────────────────────────────────────────
    String getName();

    String getDescription();

    // ── Event subscription ────────────────────────────────────────────────────
    default java.util.Set<PassiveEvent> respondsTo() {
        return java.util.EnumSet.of(PassiveEvent.TICK);
    }

    // ── Timer interval (only used when TICK is in respondsTo()) ───────────────
    default int getIntervalMs() { return 0; }

    // ── Core hook ─────────────────────────────────────────────────────────────
    void trigger(PassiveContext ctx);

    // ── Optional lifecycle hooks ──────────────────────────────────────────────
    default void onBattleStart(Entity owner, IBattle battle) {}

    default void onBattleEnd(Entity owner, IBattle battle) {}
}
