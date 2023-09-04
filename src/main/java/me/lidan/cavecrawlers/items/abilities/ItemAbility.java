package me.lidan.cavecrawlers.items.abilities;

import lombok.Getter;

@Getter
public abstract class ItemAbility {
    private final String name;
    private final String description;
    private final double cost;
    private final double cooldown;

    public ItemAbility(String name, String description, double cost, double cooldown) {
        this.name = name;
        this.description = description;
        this.cost = cost;
        this.cooldown = cooldown;
    }

    public abstract void useAbility();
}
