package Entities;

public abstract class Enemy extends Entity {
    public static final double BASE_ACCURACY = 0.85;

    public Enemy(String name, int maxHp, int attack, int defense, int attackSpeed, double critRate, double critDamage) {
        super(name, maxHp, attack, defense, attackSpeed, critRate, critDamage, BASE_ACCURACY);
    }
}