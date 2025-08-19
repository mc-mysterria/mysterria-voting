package net.mysterria.voting.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

    private final static MiniMessage mm = MiniMessage.miniMessage();
    private static TranslationManager translationManager;

    public static void setTranslationManager(TranslationManager manager) {
        translationManager = manager;
    }

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

    public static @NotNull Component formatTranslatedMessage(Player player, String path, Map<String, String> placeholders) {
        if (translationManager == null) {
            return formatMessage(path, placeholders);
        }
        String message = translationManager.getTranslation(player, path, path);
        return formatMessage(message, placeholders);
    }

    public static @NotNull String formatPlain(String message, Map<String, String> placeholders) {
        return PlainTextComponentSerializer.plainText().serialize(formatMessage(message, placeholders));
    }

    public static @NotNull String formatTranslatedPlain(Player player, String path, Map<String, String> placeholders) {
        return PlainTextComponentSerializer.plainText().serialize(formatTranslatedMessage(player, path, placeholders));
    }

    public static void sendTitle(Player player, @Nullable String titleText, @Nullable String subtitleText, Map<String, String> placeholders) {
        Component title = titleText != null ? 
            formatMessage(titleText, placeholders) : 
            Component.empty();
        Component subtitle = subtitleText != null ? 
            formatMessage(subtitleText, placeholders) : 
            Component.empty();
            
        Title titleObj = Title.title(title, subtitle, 
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500)));
        player.showTitle(titleObj);
    }
}