package Entities;

public abstract class Enemy extends Entity {
    protected int    karmaReward;
    protected int    goldReward;
    protected String rank;

    public static final double BASE_ACCURACY = 0.85;

    public Enemy(String name, int maxHp, int attack, int defense,
                 int attackSpeed, double critRate, double critDamage,
                 String rank, int karmaReward, int goldReward) {
        super(name, maxHp, attack, defense, attackSpeed, critRate, critDamage, BASE_ACCURACY);
        this.rank        = rank;
        this.karmaReward = karmaReward;
        this.goldReward  = goldReward;
    }

    public int    getKarmaReward() { return karmaReward; }
    public int    getGoldReward()  { return goldReward; }
    public String getRank()        { return rank; }

    public abstract String getTaunt();
}