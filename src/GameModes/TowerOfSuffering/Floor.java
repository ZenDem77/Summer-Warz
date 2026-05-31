package GameModes.TowerOfSuffering;

import Entities.Enemy;

import java.util.List;
import java.util.function.Supplier;

public class Floor {

    private final int              floorNumber;
    private final boolean          isBoss;
    private final Supplier<List<Enemy>> enemyFactory;

    public Floor(int floorNumber, Supplier<List<Enemy>> enemyFactory) {
        this.floorNumber  = floorNumber;
        this.isBoss       = floorNumber % 5 == 0;
        this.enemyFactory = enemyFactory;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public int     getFloorNumber()  { return floorNumber; }
    public boolean isBoss()          { return isBoss; }

    public List<Enemy> createEnemies() {
        return enemyFactory.get();
    }

    @Override
    public String toString() {
        return "Floor " + floorNumber + (isBoss ? " [BOSS]" : "");
    }
}
