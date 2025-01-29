package me.lidan.cavecrawlers.utils;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

/**
 * Cooldown class to manage cooldowns
 * How to use:
 * Make a new instance of the cooldown class with the key type and the cooldown time
 * Start the cooldown for a key
 * when you want to check if the cooldown is over, call isCooldownFinished
 * If you want to reset the cooldown, call resetCooldown
 * If you want to start the cooldown, call startCooldown
 *
 * @param <T> the key type
 */
@ToString
public class Cooldown<T> {
    private final Map<T, Long> cooldowns = new HashMap<>();
    @Setter
    @Getter
    private long cooldown;

    public Cooldown() {
        cooldown = 0L;
    }

    public Cooldown(Long cooldown) {
        this.cooldown = cooldown;
    }

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

    public void resetCooldown(T key) {
        setCooldown(key, 0L);
    }

    public long getCooldown(T key) {
        return cooldowns.getOrDefault(key, 0L);
    }

    public long getCurrentCooldown(T key) {
        return System.currentTimeMillis() - this.getCooldown(key);
    }

    public boolean isCooldownFinished(T key) throws IllegalArgumentException {
        return isCooldownFinished(key, cooldown);
    }

    public boolean isCooldownFinished(T key, long cooldown) throws IllegalArgumentException {
        if (cooldown == 0L)
            throw new IllegalArgumentException("Cooldown not set");
        Long currentCD = cooldowns.get(key);
        if (currentCD == null)
            return true;
        if (getCurrentCooldown(key) >= cooldown) {
            cooldowns.remove(key);
            return true;
        }
        return false;
    }
}
