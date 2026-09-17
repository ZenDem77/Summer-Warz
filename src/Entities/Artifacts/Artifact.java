package Entities.Artifacts;

import Entities.StatType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Artifact {

    // ── Substat pool ─────────────────────────────────────────────────────────
    private static final java.util.Map<StatType, double[]> SUBSTAT_POOL =
            java.util.Map.of(
                    StatType.CRIT_RATE,   new double[]{ 0.04, 0.08, 0.12, 0.16 },
                    StatType.CRIT_DAMAGE, new double[]{ 0.08, 0.16, 0.24, 0.32 },
                    StatType.ATK,         new double[]{ 12,   24,   36,   48   },
                    StatType.ATK_PERCENT, new double[]{ 0.08, 0.16, 0.24, 0.32 },
                    StatType.DEF,         new double[]{ 4,    8,    16,   24   },
                    StatType.DEF_PERCENT, new double[]{ 0.08, 0.16, 0.24, 0.32 },
                    StatType.HP,          new double[]{ 48,   96,   144,  192  },
                    StatType.HP_PERCENT,  new double[]{ 0.04, 0.08, 0.12, 0.16 },
                    StatType.ACCURACY,    new double[]{ 0.0125, 0.025, 0.0375, 0.05 },
                    StatType.DAMAGE_BONUS, new double[]{ 0.06, 0.12, 0.18, 0.24}
            );

    private static final StatType[] ALL_TYPES = SUBSTAT_POOL.keySet().toArray(new StatType[0]);

    // ── Instance ──────────────────────────────────────────────────────────────
    private final List<ArtifactSubstat> substats;

    public Artifact(List<ArtifactSubstat> substats) {
        if (substats.size() != 4)
            throw new IllegalArgumentException("Artifact must have exactly 4 substats, got: " + substats.size());

        long distinctTypes = substats.stream().map(ArtifactSubstat::type).distinct().count();
        if (distinctTypes != 4)
            throw new IllegalArgumentException("Artifact substats must all have distinct StatTypes.");

        this.substats = List.copyOf(substats);   // defensive immutable copy
    }

    // ── Random generation ────────────────────────────────────────────────────
    public static Artifact generateRandom() {
        // Shuffle all 9 types, take the first 4 → guarantees distinct types
        List<StatType> shuffled = new ArrayList<>(Arrays.asList(ALL_TYPES));
        Collections.shuffle(shuffled);

        List<ArtifactSubstat> rolled = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            StatType type = shuffled.get(i);
            double[] possibleValues = SUBSTAT_POOL.get(type);
            double value = possibleValues[(int)(Math.random() * possibleValues.length)];
            rolled.add(new ArtifactSubstat(type, value));
        }

        return new Artifact(rolled);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public List<ArtifactSubstat> getSubstats() { return substats; }

    public double getSubstatValue(StatType type) {
        for (ArtifactSubstat s : substats) {
            if (s.type() == type) return s.value();
        }
        return 0.0;
    }

    public boolean hasSubstat(StatType type) {
        return substats.stream().anyMatch(s -> s.type() == type);
    }

    // ── Display ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Artifact [");
        for (int i = 0; i < substats.size(); i++) {
            sb.append(substats.get(i).getDisplayString());
            if (i < substats.size() - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }
}