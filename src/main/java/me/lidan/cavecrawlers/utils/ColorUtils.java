package me.lidan.cavecrawlers.utils;

import org.bukkit.Color;

import java.util.ArrayList;
import java.util.List;

public class ColorUtils {
    /**
     * Get all the minecraft colors
     *
     * @return all the minecraft colors
     */
    public static List<Color> getAllMinecraftColors(){
        List<Color> all_color = new ArrayList<>();
        all_color.add(Color.YELLOW); // yellow
        all_color.add(Color.TEAL); //cyan
        all_color.add(Color.SILVER); //light gray
        all_color.add(Color.RED); // red
        all_color.add(Color.PURPLE); //purple
        all_color.add(Color.ORANGE); //orange
        all_color.add(Color.LIME); //lime
        all_color.add(Color.GREEN); //green
        all_color.add(Color.FUCHSIA); //magenta
        all_color.add(Color.BLUE); //blue
        all_color.add(Color.AQUA); //light blue
        all_color.add(Color.WHITE); //white
        all_color.add(Color.BLACK); //black
        all_color.add(Color.GRAY); //gray
        all_color.add(Color.fromRGB(139,69,19)); //brown
        all_color.add(Color.fromRGB(255,192,203)); //pink
        return all_color;
    }

    /**
     * Get the hex color
     * @param color the color
     * @return the hex color
     */
    public static String getHexColor(Color color){
        return "#%02x%02x%02x".formatted(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Get the minecraft color from the bukkit color
     * @param color the color
     * @return the minecraft color
     */
    public static String getMinecraftColorFromBukkitColor(Color color){
        if(color.equals(Color.YELLOW))
            return "YELLOW";
        if(color.equals(Color.TEAL))
            return "CYAN";
        if(color.equals(Color.SILVER))
            return "LIGHT_GRAY";
        if(color.equals(Color.RED))
            return "RED";
        if(color.equals(Color.PURPLE))
            return "PURPLE";
        if(color.equals(Color.ORANGE))
            return "ORANGE";
        if(color.equals(Color.LIME))
            return "LIME";
        if(color.equals(Color.GREEN))
            return "GREEN";
        if(color.equals(Color.FUCHSIA))
            return "MAGENTA";
        if(color.equals(Color.BLUE))
            return "BLUE";
        if(color.equals(Color.AQUA))
            return "LIGHT_BLUE";
        if(color.equals(Color.WHITE))
            return "WHITE";
        if(color.equals(Color.BLACK))
            return "BLACK";
        if(color.equals(Color.GRAY))
            return "GRAY";
        if(color.equals(Color.fromRGB(139,69,19)))
            return "BROWN";
        if(color.equals(Color.fromRGB(255,192,203)))
            return "PINK";
        return "unknown";
    }
}
