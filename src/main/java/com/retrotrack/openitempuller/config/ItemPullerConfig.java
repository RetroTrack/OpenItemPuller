package com.retrotrack.openitempuller.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.retrotrack.openitempuller.ItemPuller.CONFIG;

public class ItemPullerConfig {

    public static final String CONFIG_DIRECTORY = FabricLoader.getInstance().getConfigDir() + "/itempuller/";
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

    // Define the Config class
    public static class Config {
        // Use a map to store key-value pairs for properties
        private final Map<String, Object> properties;

        public Config() {
            properties = new HashMap<>();
            // Set default values
            properties.put("radius", 128);
            properties.put("priority_type", 0);
        }

        public void addProperty(String key, Object value) {
            properties.put(key, value);
        }
        // Get a property value by key
        public Object getProperty(String key) {
            return properties.get(key);
        }
        public Integer getInteger(String key) {
            Object value = properties.get(key);
            if (value instanceof Double) return ((Double) value).intValue();
            else if (value instanceof Integer) return (Integer) value;
            else if (value instanceof Float) return ((Float) value).intValue();
            else return null;
        }

        public String getString(String key) {
            return properties.get(key).toString();
        }
        public void fillDefaultValues() {
            // Fill in default values if they are missing
            if (!properties.containsKey("radius")) {
                properties.put("radius", 16);
            }
            if (!properties.containsKey("priority_type")) {
                properties.put("priority_type", 0);
            }
        }

        // You can add more methods as needed to access or manipulate properties
    }
}
