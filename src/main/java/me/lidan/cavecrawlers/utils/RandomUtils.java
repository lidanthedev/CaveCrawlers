package me.lidan.cavecrawlers.utils;

import java.util.List;

/**
 * RandomUtils class to manage random utils
 * Making it easier to generate random numbers and chances
 */
public class RandomUtils {
    /**
     * Generate a random int between min and max
     *
     * @param min the minimum value
     * @param max the maximum value
     * @return the random int
     */
    public static int randomInt(int min, int max) {
        max++;
        min *= 1000;
        max *= 1000;
        return (int) (Math.random() * (max - min) + min) / 1000;
    }

    /**
     * Generate a random double between min and max
     * @param min the minimum value
     * @param max the maximum value
     * @return the random double
     */
    public static double randomDouble(double min, double max) {
        min *= 1000;
        max *= 1000;
        return (Math.random() * (max - min) + min) / 1000;
    }

    /**
     * Generate a random boolean
     * @param percent the chance of the boolean being true
     * @return the random boolean
     */
    public static boolean chanceOf(double percent) {
        double chance = Math.random() * 100;
        return chance <= percent;
    }

    /**
     * Get a random value from a list of values
     * @param ts the list of values
     * @return the random value
     * @param <T> the type of the values
     */
    @SafeVarargs
    public static <T> T getRandom(T... ts) {
        int random = randomInt(0, ts.length - 1);
        return ts[random];
    }

    /**
     * Get a random value from a list
     *
     * @param list the list of values
     * @param <T>  the type of the values
     * @return the random value
     */
    public static <T> T getRandom(List<T> list) {
        int random = randomInt(0, list.size() - 1);
        return list.get(random);
    }
}
