package Entities;

public abstract class Character extends Entity {
    protected int    level;
    protected int    karma;
    protected String clan;

    public Character(String name, int maxHp, int attack, int defense,
                     int attackSpeed, int level, String clan) {
        super(name, maxHp, attack, defense, attackSpeed);
        this.level = level;
        this.karma = 0;
        this.clan  = clan;
    }

    public void gainKarma(int amount) { this.karma += amount; }

    public int    getLevel() { return level; }
    public int    getKarma() { return karma; }
    public String getClan()  { return clan; }

    public abstract String getSpecialMoveName();
}