package Combat;

public class DamageResult {
    public final int     amount;
    public final boolean isCrit;
    public final boolean isMiss;

    public DamageResult(int amount, boolean isCrit, boolean isMiss) {
        this.amount = amount;
        this.isCrit = isCrit;
        this.isMiss = isMiss;
    }
}