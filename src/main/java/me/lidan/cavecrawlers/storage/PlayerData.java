package me.lidan.cavecrawlers.storage;

import me.lidan.cavecrawlers.skills.Skills;

public class PlayerData {

    private Skills skills;

    public PlayerData() {
        this(new Skills());
    }

    public PlayerData(Skills skills) {
        this.skills = skills;
    }

    public Skills getSkills() {
        return skills;
    }

    public void setSkills(Skills skills) {
        this.skills = skills;
    }
}
