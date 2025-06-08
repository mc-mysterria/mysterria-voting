package oluni.official.minecraft.oWOVoting.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {
    private final static MiniMessage mm = MiniMessage.miniMessage();

    public static @NotNull Component formatMessage(String message, Map<String, String> placeholders) {
        if (placeholders != null) {
            Matcher matcher = Pattern.compile("\\{([^}]+)}").matcher(message);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String placeholder = matcher.group(1);
                String replacement = placeholders.getOrDefault(placeholder, matcher.group());
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            message = sb.toString();
        }
        return mm.deserialize(message);
    }

    public static @NotNull String formatPlain(String message, Map<String, String> placeholders) {
        return PlainTextComponentSerializer.plainText().serialize(formatMessage(message, placeholders));
    }
}