package Combat;

public class DamageResult {
    public final int     amount;
    public final boolean isCrit;

    public DamageResult(int amount, boolean isCrit) {
        this.amount = amount;
        this.isCrit = isCrit;
    }
}