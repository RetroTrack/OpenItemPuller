package com.retrotrack.openitempuller.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.retrotrack.openitempuller.ItemPuller.CONFIG;
import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

public class ItemPullerConfig {

    public static final String CONFIG_DIRECTORY = FabricLoader.getInstance().getConfigDir() + "/" + MOD_ID + "/";
    public static final String CONFIG_FILE = CONFIG_DIRECTORY + "config.json";

    public static void initConfig() {
        // Load existing configuration or create a new one
        CONFIG = loadConfig(CONFIG_FILE);
        saveConfig(CONFIG, CONFIG_FILE);
    }

    public static Config loadConfig(String filename) {
        try {
            // Read the JSON file into a string
            String content = new String(Files.readAllBytes(Path.of(filename)));

            // Parse JSON into Config object
            Gson gson = new Gson();
            Config config = gson.fromJson(content, Config.class);

            // Fill in default values if they are missing
            config.fillDefaultValues();

            return config;
        } catch (IOException e) {
            // If the file doesn't exist or cannot be read, create a new Config object
            return new Config();
        }
    }

    public static void saveConfig(Config config, String filename) {
        try {
            // Create directories if they don't exist
            File directory = new File(CONFIG_DIRECTORY);
            if (!directory.exists()) {
                directory.mkdirs();
            }


            // Write JSON to file
            FileWriter writer = new FileWriter(filename);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(config, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveSettings(int radius, int priorityType, String sortingMode, int displayMode) {
        CONFIG.putProperty("radius", radius);
        CONFIG.putProperty("priority_type", priorityType);
        CONFIG.putProperty("sorting_mode", sortingMode);
        CONFIG.putProperty("display_mode", displayMode);
        saveConfig(CONFIG, ItemPullerConfig.CONFIG_FILE);
    }

    // Define the Config class
    public static class Config {
        // Use a map to store key-value pairs for properties
        private final Map<String, Object> properties;

        public Config() {
            properties = new HashMap<>();
            // Set default values
            properties.put("radius", 16);
            properties.put("priority_type", 0);
            properties.put("display_mode", 0);
            properties.put("sorting_mode", "descending");
            properties.put("is_target_block", false);
            properties.put("target_block_pos", new BlockPos(0,0,0));
        }

        public void fillDefaultValues() {
            // Fill in default values if they are missing
            if (!properties.containsKey("radius")) {
                properties.put("radius", 16);
            }
            if (!properties.containsKey("priority_type")) {
                properties.put("priority_type", 0);
            }
            if (!properties.containsKey("display_mode")) {
                properties.put("display_mode", 0);
            }
            if (!properties.containsKey("sorting_mode")) {
                properties.put("sorting_mode", "descending");
            }
            if (!properties.containsKey("is_target_block")) {
                properties.put("is_target_block", false);
            }
            if (!properties.containsKey("target_block_pos")) {
                properties.put("target_block_pos", new BlockPos(0,0,0));
            }
        }

        public void putProperty(String key, Object value) {
            properties.put(key, value);
        }
        // Get a property value by key
        public Object getProperty(String key) {
            return properties.get(key);
        }

        public Integer getInteger(String key) {
            Object value = properties.get(key);
            return switch (value) {
                case Double v -> v.intValue();
                case Integer i -> i;
                case Float v -> v.intValue();
                case null, default -> null;
            };
        }

        public Boolean getBoolean(String key) {
            Object value = properties.get(key);
            if(value instanceof Boolean) return (Boolean) value;
            return null;
        }


        public String getString(String key) {
            return properties.get(key).toString();
        }

        // You can add more methods as needed to access or manipulate properties
    }
}
