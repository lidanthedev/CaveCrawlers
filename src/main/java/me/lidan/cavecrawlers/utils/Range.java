package me.lidan.cavecrawlers.utils;

import lombok.Getter;

@Getter
public class Range {
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

    public boolean isInRange(int num){
        return num >= min && num <= max;
    }

    @Override
    public String toString() {
        return min + "-" + max;
    }
}
