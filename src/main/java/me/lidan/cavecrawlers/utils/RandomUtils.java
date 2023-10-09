package me.lidan.cavecrawlers.utils;

public class RandomUtils {
    public static int randomInt(int min, int max) {
        max++;
        min *= 1000;
        max *= 1000;
        return (int) (Math.random() * (max - min) + min) / 1000;
    }

    public static double randomDouble(double min, double max) {
        min *= 1000;
        max *= 1000;
        return (Math.random() * (max - min) + min) / 1000;
    }

    public static boolean chanceOf(double percent) {
        double chance = Math.random() * 100;
        return chance <= percent;
    }

    @SafeVarargs
    public static <T> T getRandom(T... ts) {
        int random = randomInt(0, ts.length - 1);
        return ts[random];
    }
}
