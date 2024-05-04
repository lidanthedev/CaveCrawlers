package me.lidan.cavecrawlers.utils;

import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
public class Cooldown<T> {
    private final Map<T, Long> cooldowns = new HashMap<>();

    public void setCooldown(T key, Long time) {
        if (time < 1) {
            cooldowns.remove(key);
        } else {
            cooldowns.put(key, time);
        }
    }

    public void startCooldown(T key){
        setCooldown(key, System.currentTimeMillis());
    }

    public long getCooldown(T key) {
        return cooldowns.getOrDefault(key, 0L);
    }

    public long getCurrentCooldown(T key) {
        return System.currentTimeMillis() - this.getCooldown(key);
    }
}
