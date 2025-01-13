package me.lidan.cavecrawlers.utils;

import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * Cooldown class to manage cooldowns
 * How to use:
 * Make a new instance of the cooldown class
 * Set the cooldown for a key
 * when you want to check if the cooldown is over, check if the current cooldown is greater than the cooldown
 * If you want to reset the cooldown, set the cooldown to 0
 * If you want to start the cooldown, set the cooldown to the current time
 *
 * @param <T> the key type
 */
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
