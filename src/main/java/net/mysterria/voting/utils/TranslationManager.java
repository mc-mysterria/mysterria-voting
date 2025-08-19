package net.mysterria.voting.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class TranslationManager {
    private final Plugin plugin;
    public final Map<String, FileConfiguration> translations = new HashMap<>();
    
    public TranslationManager(Plugin plugin) {
        this.plugin = plugin;
        loadTranslations();
    }
    
    private void loadTranslations() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        
        copyDefaultLanguageFiles(langFolder);
        loadLanguageFiles(langFolder);
    }
    
    private void copyDefaultLanguageFiles(File langFolder) {
        String[] languages = {"en", "uk"};
        for (String lang : languages) {
            File langFile = new File(langFolder, lang + ".yml");
            if (!langFile.exists()) {
                try (InputStream in = plugin.getResource("lang/" + lang + ".yml")) {
                    if (in != null) {
                        Files.copy(in, langFile.toPath());
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("Could not copy default language file: " + lang + ".yml");
                }
            }
        }
    }
    
    private void loadLanguageFiles(File langFolder) {
        File[] langFiles = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (langFiles != null) {
            for (File file : langFiles) {
                String langCode = file.getName().replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                translations.put(langCode, config);
            }
        }
    }
    
    public String getTranslation(Player player, String path, String defaultValue) {
        String playerLocale = getPlayerLocale(player);
        FileConfiguration config = translations.get(playerLocale);
        
        if (config != null && config.contains(path)) {
            return config.getString(path);
        }
        
        FileConfiguration defaultConfig = translations.get("en");
        if (defaultConfig != null && defaultConfig.contains(path)) {
            return defaultConfig.getString(path);
        }
        
        return defaultValue != null ? defaultValue : path;
    }
    
    public FileConfiguration getTranslationConfig(Player player) {
        String playerLocale = getPlayerLocale(player);
        FileConfiguration config = translations.get(playerLocale);
        
        if (config != null) {
            return config;
        }
        
        return translations.get("en");
    }
    
    private String getPlayerLocale(Player player) {
        String locale = player.locale().getLanguage();
        if (translations.containsKey(locale)) {
            return locale;
        }
        return "en";
    }
    
    public void reload() {
        translations.clear();
        loadTranslations();
    }
}