package me.lidan.cavecrawlers.utils;

import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class StringUtils {
    public static String setTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder converted = new StringBuilder();

        boolean convertNext = true;
        for (char ch : text.toCharArray()) {
            if (Character.isSpaceChar(ch)) {
                convertNext = true;
            } else if (convertNext) {
                ch = Character.toTitleCase(ch);
                convertNext = false;
            } else {
                ch = Character.toLowerCase(ch);
            }
            converted.append(ch);
        }

        return converted.toString();
    }

    public static String progressBar(double current, double max, int length) {
        return progressBar(current, max, length, "-");
    }

    public static String progressBar(double current, double max, int length, String icon) {
        StringBuilder progressBar = new StringBuilder();
        double percent = current / max * 100d;
        for (int i = 0; i < length; i++) {
            if (i < percent / (100d / length)) {
                progressBar.append(ChatColor.GREEN);
            } else {
                progressBar.append(ChatColor.WHITE);
            }
            progressBar.append(icon);
        }
        return progressBar.toString();
    }

    public static String getWhatAfterNumber(int num) {
        int last = Integer.parseInt(((num + "").charAt((num + "").length() - 1)) + "");
        if (last == 1) return "st";
        else if (last == 2) return "nd";
        else if (last == 3) return "rd";
        else return "th";
    }

    public static String rainbowText(String text){
        StringBuilder b = new StringBuilder();
        ChatColor[] ch = {ChatColor.WHITE, ChatColor.YELLOW, ChatColor.GOLD, ChatColor.RED, ChatColor.RED, ChatColor.WHITE};
        int i = 0;
        for (char c : text.toCharArray()) {
            b.append(ch[i]).append(c);
            i++;
            i = (i+ch.length) % ch.length; //modulus is amazing use it more
        }      //             ^------------------------------<
        return b.toString();
    }

    public static String getPlural(String text, int num) {
        if (num == 1) return text;
        String[] texts = text.split("");
        if (texts[texts.length - 1].equalsIgnoreCase("s") || (texts[texts.length - 2].equalsIgnoreCase("s") && texts[texts.length - 1].equalsIgnoreCase("h")) || (texts[texts.length - 2].equalsIgnoreCase("c") && texts[texts.length - 1].equalsIgnoreCase("h")) || texts[texts.length - 1].equalsIgnoreCase("x") || texts[texts.length - 1].equalsIgnoreCase("z")) {
            text += "es";
        } else if ((texts[texts.length - 2].equalsIgnoreCase("a") || texts[texts.length - 2].equalsIgnoreCase("e") || texts[texts.length - 2].equalsIgnoreCase("o") || texts[texts.length - 2].equalsIgnoreCase("u") || texts[texts.length - 2].equalsIgnoreCase("i")) && !(texts[texts.length - 1].equalsIgnoreCase("a") || texts[texts.length - 1].equalsIgnoreCase("e") || texts[texts.length - 1].equalsIgnoreCase("o") || texts[texts.length - 1].equalsIgnoreCase("u") || texts[texts.length - 1].equalsIgnoreCase("i")) && !(texts[texts.length - 3].equalsIgnoreCase("a") || texts[texts.length - 3].equalsIgnoreCase("e") || texts[texts.length - 3].equalsIgnoreCase("o") || texts[texts.length - 3].equalsIgnoreCase("u") || texts[texts.length - 3].equalsIgnoreCase("i"))) {
            text += texts[texts.length - 1] + "es";
        } else if ((texts[texts.length - 1].equalsIgnoreCase("f") || (texts[texts.length - 2].equalsIgnoreCase("f") && texts[texts.length - 1].equalsIgnoreCase("e"))) && !(text.equalsIgnoreCase("roof") || text.equalsIgnoreCase("belief") || text.equalsIgnoreCase("chef") || text.equalsIgnoreCase("chief"))) {
            text = "";
            int times = 0;
            int length = texts.length;
            if (texts[texts.length - 1].equalsIgnoreCase("e")) length -= 1;
            for (String word : texts) {
                times++;
                if (times >= length) {
                    text += "ves";
                } else {
                    text += word;
                }
            }
        } else if (texts[texts.length - 1].equalsIgnoreCase("y") && !(texts[texts.length - 2].equalsIgnoreCase("a") || texts[texts.length - 2].equalsIgnoreCase("e") || texts[texts.length - 2].equalsIgnoreCase("o") || texts[texts.length - 2].equalsIgnoreCase("u") || texts[texts.length - 2].equalsIgnoreCase("i"))) {
            int times = 0;
            for (String word : texts) {
                text = "";
                times++;
                if (times >= texts.length) {
                    text += "ies";
                } else {
                    text += word;
                }
            }
        } else if ((texts[texts.length - 1].equalsIgnoreCase("a") || texts[texts.length - 1].equalsIgnoreCase("e") || texts[texts.length - 1].equalsIgnoreCase("o") || texts[texts.length - 1].equalsIgnoreCase("u") || texts[texts.length - 1].equalsIgnoreCase("i")) && !(text.equalsIgnoreCase("photo") || text.equalsIgnoreCase("piano") || text.equalsIgnoreCase("halo") || text.equalsIgnoreCase("volcano"))) {
            text += "es";
        } else if (texts[texts.length - 2].equalsIgnoreCase("u") && texts[texts.length - 1].equalsIgnoreCase("s")) {
            int times = 0;
            for (String word : texts) {
                text = "";
                times++;
                if (times > texts.length - 1) {
                } else if (times == texts.length - 1) {
                    text += "i";
                } else {
                    text += word;
                }
            }
        } else if (texts[texts.length - 2].equalsIgnoreCase("i") && texts[texts.length - 1].equalsIgnoreCase("s")) {
            int times = 0;
            for (String word : texts) {
                text = "";
                times++;
                if (times == texts.length) {
                    text += "s";
                } else if (times == texts.length - 1) {
                    text += "e";
                } else {
                    text += word;
                }
            }
        } else if (texts[texts.length - 2].equalsIgnoreCase("o") && texts[texts.length - 1].equalsIgnoreCase("n")) {
            int times = 0;
            for (String word : texts) {
                text = "";
                times++;
                if (times > texts.length - 1) {
                } else if (times == texts.length - 1) {
                    text += "a";
                } else {
                    text += word;
                }
            }
        } else if (text.equalsIgnoreCase("sheep") || text.equalsIgnoreCase("series") || text.equalsIgnoreCase("species") || text.equalsIgnoreCase("deer") || text.equalsIgnoreCase("fish")) {
        } else if (text.equalsIgnoreCase("child")) text = "children";
        else if (text.equalsIgnoreCase("goose")) text = "geese";
        else if (text.equalsIgnoreCase("man")) text = "men";
        else if (text.equalsIgnoreCase("woman")) text = "women";
        else if (text.equalsIgnoreCase("tooth")) text = "teeth";
        else if (text.equalsIgnoreCase("foot")) text = "feet";
        else if (text.equalsIgnoreCase("mouse")) text = "mice";
        else if (text.equalsIgnoreCase("person")) text = "people";
        else {
            text += "s";
        }
        return text;
    }

    public static String getColorName(int color) {
        switch (color) {
            case 0:
                return "White";
            case 1:
                return "Orange";
            case 2:
                return "Magenta";
            case 3:
                return "Light BLue";
            case 4:
                return "Yellow";
            case 5:
                return "Lime";
            case 6:
                return "Pink";
            case 7:
                return "Gray";
            case 8:
                return "Light Gray";
            case 9:
                return "Cyan";
            case 10:
                return "Purple";
            case 11:
                return "Blue";
            case 12:
                return "Brown";
            case 13:
                return "Green";
            case 14:
                return "Red";
            case 15:
                return "Black";
        }
        return "";
    }

    @NotNull
    public static String getNumberWithoutDot(double value) {
        String strValue = "" + value;
        String[] splitValue = strValue.split("\\.");
        return splitValue[0];
    }

    public static List<String> loreBuilder(String lore, ChatColor color, int num) {
        String[] lores = lore.split(" ");
        int l = 0;
        int n = 0;
        ArrayList<String> list = new ArrayList<>();
        for (String str : lores) {
            if (str.equals("RESET_LENGTH")) {
                l = 0;
            } else if (str.equals("NEW_LINE")) {
                list.add(color + "");
                l = 0;
                n++;
            } else {
                l += str.length() + 1;
                try {
                    list.set(n, list.get(n) + str + " ");
                } catch (IndexOutOfBoundsException ex) {
                    list.add(color + "");
                    list.set(n, list.get(n) + str + " ");
                }
                if (l > num) {
                    l = 0;
                    n++;
                }
            }
        }
        return list;
    }

    public static List<String> loreBuilder(String lore) {
        return loreBuilder(lore, ChatColor.GRAY, 30);
    }

    public static <T> String getNumberFormat(Number num) {
        return NumberFormat.getNumberInstance(Locale.US).format(num);
    }
}
