package me.lidan.cavecrawlers.utils;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Getter
public class Range implements Iterable<Integer> {
    private final int min;
    private final int max;

    public Range(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public Range(String amountStr){
        if (amountStr.contains("-")){
            String[] arr = amountStr.split("-");
            min = Integer.parseInt(arr[0]);
            max = Integer.parseInt(arr[1]);
        }
        else{
            min = Integer.parseInt(amountStr);
            max = Integer.parseInt(amountStr);
        }
    }

    public int getRandom(){
        return RandomUtils.randomInt(min, max);
    }

    public boolean isInRange(int num){
        return num >= min && num <= max;
    }

    public List<Integer> getRange() {
        List<Integer> range = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            range.add(i);
        }
        return range;
    }

    @Override
    public @NotNull Iterator<Integer> iterator() {
        return getRange().iterator();
    }

    @Override
    public String toString() {
        return min + "-" + max;
    }
}
