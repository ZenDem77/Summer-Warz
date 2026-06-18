package Combat;

import Entities.Entity;
import Entities.PassiveHandler.*;

public interface IBattle {

    // ── Entity access ─────────────────────────────────────────────────────────
    Entity getPlayer();

    Entity getEnemy();

    default Entity getOpponent(Entity owner) {
        return owner == getPlayer() ? getEnemy() : getPlayer();
    }

    // ── Passive event broadcasting ────────────────────────────────────────────
    int applyPassiveEvent(Entity owner, Entity target, PassiveEvent event, int damage, boolean isCrit);

    // ── Notifications ─────────────────────────────────────────────────────────
    void notifyPassive(Entity owner, Entity target, String passiveName,
                       String effectDesc, int amount, boolean isHeal);

    void checkEndPublic();

    void notifyShield(Entity owner, String passiveName, int amount);

    void notifyPassiveMiss(Entity owner, Entity target, String passiveName);

    default double getDamageMultiplier() { return 1.0; }
}