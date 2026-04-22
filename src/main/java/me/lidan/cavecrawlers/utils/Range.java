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
        if (amountStr.contains("-") && amountStr.indexOf('-', 1) != -1){
            int delimIndex = amountStr.indexOf('-', 1);
            String left = amountStr.substring(0, delimIndex);
            String right = amountStr.substring(delimIndex + 1);
            min = Long.parseLong(left);
            max = Long.parseLong(right);
        }
        else{
            min = Long.parseLong(amountStr);
            max = Long.parseLong(amountStr);
        }
    }

    public long getRandom() {
        return RandomUtils.randomLong(min, max);
    }

    public boolean isInRange(long num){
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
        return new Iterator<Long>() {
            private long current = min;
            private boolean finished = min > max;

            @Override
            public boolean hasNext() {
                return !finished;
            }

            @Override
            public Long next() {
                if (!hasNext()) {
                    throw new java.util.NoSuchElementException();
                }
                long result = current;
                if (current == max) {
                    finished = true;
                } else {
                    current++;
                }
                return result;
            }
        };
    }

    @Override
    public String toString() {
        if (min == max) {
            return String.valueOf(min);
        }
        return min + "-" + max;
    }
}