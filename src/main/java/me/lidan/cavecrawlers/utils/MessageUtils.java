package me.lidan.cavecrawlers.utils;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;

public class MessageUtils {
    private final static int CENTER_PX = 100;
    private static final int CHAT_WIDTH_PX = (int) Math.floor(CENTER_PX * 1.7); // Total chat width in pixels
    private static final int SPACE_WIDTH_PX = DefaultFontInfo.SPACE.getLength(); // Width of a single space

    /**
     * Creates a horizontal "line" made of spaces that spans the player's chat window.
     *
     * @return A Component representing the horizontal line of spaces.
     */
    public static String spaceLine() {
        int spaceCount = CHAT_WIDTH_PX / SPACE_WIDTH_PX; // Number of spaces to fill the chat window

        return "<st><dark_gray>"+" ".repeat(Math.max(0, spaceCount)) + "</dark_gray></st>";
    }

    /**
     * Centers a message in the player's chat window.
     *
     * @param message The message to center.
     * @return A centered message.
     */
    public static String CenteredMessageWithMinecraftSymbols(String message) {
        String[] lines = ChatColor.translateAlternateColorCodes('&', message).split("\n", 40);
        StringBuilder returnMessage = new StringBuilder();


        for (String line : lines) {
            int messagePxSize = 0;
            boolean previousCode = false;
            boolean isBold = false;

            for (char c : line.toCharArray()) {
                if (c == '§') {
                    previousCode = true;
                } else if (previousCode) {
                    previousCode = false;
                    isBold = c == 'l';
                } else {
                    DefaultFontInfo dFI = DefaultFontInfo.getDefaultFontInfo(c);
                    messagePxSize = isBold ? messagePxSize + dFI.getBoldLength() : messagePxSize + dFI.getLength();
                    messagePxSize++;
                }
            }
            int toCompensate = CENTER_PX - messagePxSize / 2;
            int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
            int compensated = 0;
            StringBuilder sb = new StringBuilder();
            while(compensated < toCompensate){
                sb.append(" ");
                compensated += spaceLength;
            }
            returnMessage.append(sb.toString()).append(line).append("\n");
        }

        return returnMessage.toString();
    }

    /**
     * Centers a message in the player's chat window.
     * @param miniMessage The message to center.
     * @return A centered message.
     */
    public static Component CenteredMessageWithMiniMessage(String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) {
            return Component.empty();
        }

        // Split the message into lines and process each line
        String[] lines = miniMessage.split("\n", 40);
        StringBuilder centeredMiniMessage = new StringBuilder();

        for (String line : lines) {
            // Parse the line to preserve formatting
            Component parsedComponent = MiniMessage.miniMessage().deserialize(line);

            // Get the plain text for pixel size calculations
            String plainText = PlainTextComponentSerializer.plainText().serialize(parsedComponent);

            int messagePxSize = 0;
            boolean isBold = false;

            // Calculate the pixel width of the line
            for (char c : plainText.toCharArray()) {
                DefaultFontInfo fontInfo = DefaultFontInfo.getDefaultFontInfo(c);
                messagePxSize += isBold ? fontInfo.getBoldLength() : fontInfo.getLength();
                messagePxSize++;
            }

            // Calculate padding to center the line
            int toCompensate = CENTER_PX - messagePxSize / 2;
            int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
            int compensated = 0;
            StringBuilder padding = new StringBuilder();

            while (compensated < toCompensate) {
                padding.append(" ");
                compensated += spaceLength;
            }

            // Add padding spaces before the line and preserve formatting
            centeredMiniMessage.append(padding).append(line).append("\n");
        }

        // Convert the centered MiniMessage back to an Adventure Component
        return MiniMessage.miniMessage().deserialize(centeredMiniMessage.toString());
    }

    /**
     * Centers a message in the player's chat window.
     *
     * @param inputComponent The message to center.
     * @return A centered message.
     */
    public static Component CenteredMessageWithComponent(Component inputComponent) {
        if (inputComponent == null) {
            return Component.empty();
        }

        // Serialize the input component to plain text for pixel size calculations
        String plainText = PlainTextComponentSerializer.plainText().serialize(inputComponent);

        int messagePxSize = 0;
        boolean isBold = false;

        // Calculate the pixel width of the plain text
        for (char c : plainText.toCharArray()) {
            DefaultFontInfo fontInfo = DefaultFontInfo.getDefaultFontInfo(c);
            messagePxSize += isBold ? fontInfo.getBoldLength() : fontInfo.getLength();
            messagePxSize++;
        }

        // Calculate padding to center the message
        int toCompensate = CENTER_PX - messagePxSize / 2;
        int spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder padding = new StringBuilder();

        while (compensated < toCompensate) {
            padding.append(" ");
            compensated += spaceLength;
        }

        // Add the calculated padding to the original component

        // Deserialize the padded text back into a Component with MiniMessage for rendering
        return Component.text(padding.toString())
                .append(inputComponent);
    }
}
