package me.lidan.cavecrawlers.utils;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Getter
public class Range implements Iterable<Long> {
    private final long min;
    private final long max;

    public Range(long min, long max) {
        this.min = min;
        this.max = max;
    }

    public Range(String amountStr){
        if (amountStr.contains("-")){
            String[] arr = amountStr.split("-");
            min = Long.parseLong(arr[0]);
            max = Long.parseLong(arr[1]);
        }
        else{
            min = Long.parseLong(amountStr);
            max = Long.parseLong(amountStr);
        }
    }

    public long getRandom() {
        return RandomUtils.randomLong(min, max);
    }

    public boolean isInRange(int num){
        return num >= min && num <= max;
    }

    public List<Long> getRangeAsList() {
        List<Long> range = new ArrayList<>();
        for (long i = min; i <= max; i++) {
            range.add(i);
        }
        return range;
    }

    @Override
    public @NotNull Iterator<Long> iterator() {
        return getRangeAsList().iterator();
    }

    @Override
    public String toString() {
        if (min == max) {
            return String.valueOf(min);
        }
        return min + "-" + max;
    }
}
