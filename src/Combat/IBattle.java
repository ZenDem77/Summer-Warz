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

    void notifySpecialHit(Entity owner, Entity target, String passiveName,
                          String effectDesc, int amount, boolean isCrit);

    void notifyImmune(Entity owner, Entity target, String passiveName);

    default int applyTrueDamageRaw(Entity owner, Entity target, int scaledAmount, String passiveName) {
        if (target.isTrueDamageImmune()) {
            notifyImmune(owner, target, passiveName);
            return -1;
        }
        double reduction   = target.getTrueDamageReduction();
        int    finalAmount = reduction > 0
                ? Math.max(1, (int)(scaledAmount * (1.0 - reduction)))
                : scaledAmount;
        int actual = Math.min(finalAmount, target.getCurrentHp());
        target.takeDamage(finalAmount);
        target.onTrueDamageReceived(owner, finalAmount, this);
        return actual;
    }

    default double getDamageMultiplier() { return 1.0; }

    default void registerPausableTimer(javax.swing.Timer timer) {}

    default void unregisterPausableTimer(javax.swing.Timer timer) {}
}