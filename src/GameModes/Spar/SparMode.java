package GameModes.Spar;

import Combat.CharacterBattle.CharacterBattle;
import Entities.Character;

import java.util.ArrayList;
import java.util.List;

/**
 * SparMode — Character vs Character battle mode.
 *
 * Unlike Tower of Suffering, there's no progression, no floors, no bosses —
 * just a single 1v1 match between two chosen characters. All damage in
 * this mode is reduced by 80% (see CharacterBattle.DAMAGE_REDUCTION), since
 * both fighters are full player characters with weapons/artifacts/passives
 * and raw numbers would otherwise be far too lethal for a fair fight.
 *
 * ── How to connect to UI later ─────────────────────────────────────────────
 *
 *  1. Construct:   SparMode mode = new SparMode(zed, kaizen);
 *  2. Subscribe:   mode.addListener(yourSparModeListener);
 *  3. Start:       CharacterBattle battle = mode.startSpar();
 *  4. Wire battle: battle.addListener(yourBattleListener);
 *                  yourGamePanel.showPanel(new CharacterBattlePanel(battle));
 *  5. On end:      call mode.onBattleEnd(result) from your
 *                  CharacterBattle.BattleListener.onBattleEnd() —
 *                  the SparModeListener fires onSparEnd(winner, loser).
 *
 *  CharacterBattlePanel (already built) is fully compatible with SparMode
 *  as-is — no new panel is required to test this.
 *
 * ── Package suggestion ─────────────────────────────────────────────────────
 *  Place in: src/GameModes/Spar/SparMode.java
 *  package GameModes.Spar;
 */
public class SparMode {

    // ── State ─────────────────────────────────────────────────────────────────

    public enum State {
        IDLE,       // not started yet
        IN_BATTLE,  // the spar is currently running
        COMPLETE    // the spar has ended — call startSpar() again for a rematch
    }

    // ── Listener interface (UI hooks) ───────────────────────────────────────

    public interface SparModeListener {
        /** Fired when the spar is about to start. */
        void onSparStart(Character fighter1, Character fighter2);

        /** Fired when the spar ends, with the winner and loser identified. */
        void onSparEnd(Character winner, Character loser);
    }

    // ── Fields ────────────────────────────────────────────────────────────────

    private final Character fighter1;
    private final Character fighter2;
    private       State     state = State.IDLE;
    private final List<SparModeListener> listeners = new ArrayList<>();

    // ── Constructor ───────────────────────────────────────────────────────────

    /**
     * @param fighter1 first character
     * @param fighter2 second character — must be a different instance than fighter1
     */
    public SparMode(Character fighter1, Character fighter2) {
        if (fighter1 == null || fighter2 == null)
            throw new IllegalArgumentException("Both fighters must be non-null.");
        if (fighter1 == fighter2)
            throw new IllegalArgumentException("A character cannot spar against itself.");

        this.fighter1 = fighter1;
        this.fighter2 = fighter2;
    }

    // ── Listener management ───────────────────────────────────────────────────

    public void addListener(SparModeListener l)    { listeners.add(l); }
    public void removeListener(SparModeListener l) { listeners.remove(l); }

    // ── Core API ──────────────────────────────────────────────────────────────

    /**
     * Creates and returns a CharacterBattle for this spar.
     * The caller should add a CharacterBattle.BattleListener to detect the
     * result and call onBattleEnd() on this SparMode accordingly.
     *
     * Both fighters' HP is reset to full (CharacterBattle.start() handles this).
     */
    public CharacterBattle startSpar() {
        state = State.IN_BATTLE;
        for (SparModeListener l : listeners) l.onSparStart(fighter1, fighter2);
        return new CharacterBattle(fighter1, fighter2);
    }

    /**
     * Call this when the CharacterBattle ends.
     * Determines the winner/loser from the result and fires onSparEnd().
     *
     * @param result the final BattleState from CharacterBattle (FIGHTER1_WIN or FIGHTER2_WIN)
     */
    public void onBattleEnd(CharacterBattle.BattleState result) {
        if (state != State.IN_BATTLE) return;
        state = State.COMPLETE;

        boolean fighter1Won = result == CharacterBattle.BattleState.FIGHTER1_WIN;
        Character winner = fighter1Won ? fighter1 : fighter2;
        Character loser  = fighter1Won ? fighter2 : fighter1;

        for (SparModeListener l : listeners) l.onSparEnd(winner, loser);
    }

    /** Resets state so startSpar() can be called again for a rematch. */
    public void reset() { state = State.IDLE; }

    // ── State queries ─────────────────────────────────────────────────────────

    public State     getState()    { return state; }
    public Character getFighter1() { return fighter1; }
    public Character getFighter2() { return fighter2; }
}