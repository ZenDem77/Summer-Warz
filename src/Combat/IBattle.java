package Combat;

import Entities.Entity;
import Entities.PassiveEvent;

public interface IBattle {

    // ── Entity access ─────────────────────────────────────────────────────────
    Entity getPlayer();

    Entity getEnemy();

    default Entity getOpponent(Entity owner) {
        return owner == getPlayer() ? getEnemy() : getPlayer();
    }

    // ── Passive event broadcasting ────────────────────────────────────────────
    int applyPassiveEvent(Entity owner, Entity target, PassiveEvent event, int damage);

    // ── Notifications ─────────────────────────────────────────────────────────
    void notifyPassive(Entity owner, Entity target, String passiveName,
                       String effectDesc, int amount, boolean isHeal);

    void checkEndPublic();
}