package me.lidan.cavecrawlers.utils;

public class Range {
    private int min;
    private int max;

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

    @Override
    public String toString() {
        return min + "-" + max;
    }
}
