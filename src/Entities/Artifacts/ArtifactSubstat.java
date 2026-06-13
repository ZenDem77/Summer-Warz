package Entities.Artifacts;

import Entities.StatType;

public record ArtifactSubstat(StatType type, double value) {

    public String getDisplayString() {
        String label = switch (type) {
            case CRIT_RATE   -> "Crit Rate";
            case CRIT_DAMAGE -> "Crit Damage";
            case ATK         -> "ATK";
            case ATK_PERCENT -> "ATK%";
            case DEF         -> "DEF";
            case DEF_PERCENT -> "DEF%";
            case HP          -> "HP";
            case HP_PERCENT  -> "HP%";
            case ACCURACY    -> "Accuracy";
        };

        boolean isPercent = switch (type) {
            case CRIT_RATE, CRIT_DAMAGE, ATK_PERCENT, DEF_PERCENT, HP_PERCENT, ACCURACY -> true;
            default -> false;
        };

        String valStr = isPercent
                ? (int)(value * 100) + "%"
                : String.valueOf((int) value);

        return label + " +" + valStr;
    }

    @Override
    public String toString() { return getDisplayString(); }
}