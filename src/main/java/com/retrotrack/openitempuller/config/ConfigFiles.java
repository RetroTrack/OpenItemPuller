package com.retrotrack.openitempuller.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Supplier;

import static com.retrotrack.openitempuller.ItemPuller.LOGGER;
import static com.retrotrack.openitempuller.ItemPuller.MOD_ID;

/** Reads and writes the two typed config files. */
public final class ConfigFiles {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final Path DIRECTORY = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
    public static final Path SERVER_FILE = DIRECTORY.resolve("server.json");
    public static final Path CLIENT_FILE = DIRECTORY.resolve("client.json");
    private static final Path LEGACY_FILE = DIRECTORY.resolve("config.json");

    private ConfigFiles() {}

    public static ServerConfig loadServer() {
        return load(SERVER_FILE, ServerConfig.class, () -> ServerConfig.DEFAULT);
    }

    public static ClientConfig loadClient() {
        return load(CLIENT_FILE, ClientConfig.class, () -> ClientConfig.DEFAULT);
    }

    public static void saveClient(ClientConfig config) {
        save(CLIENT_FILE, config);
    }

    /**
     * Loads {@code file}. A missing file is created with the defaults; an unreadable or malformed
     * file is kept as {@code <name>.broken} so hand edits are not lost, and the defaults are used.
     * The file is rewritten afterwards so new fields and sanitised values show up on disk.
     */
    private static <T> T load(Path file, Class<T> type, Supplier<T> defaults) {
        T config = null;
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                config = GSON.fromJson(reader, type); // null for an empty file
            } catch (IOException | JsonParseException e) {
                LOGGER.warn("Could not read {}, using defaults (old file kept as .broken)", file, e);
                try {
                    Files.move(file, file.resolveSibling(file.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException moveError) {
                    LOGGER.warn("Could not back up {}", file, moveError);
                }
            }
        }
        if (config == null) config = defaults.get();
        save(file, config);
        return config;
    }

    private static void save(Path file, Object config) {
        try {
            Files.createDirectories(file.getParent());
            // Write to a temp file first so a crash mid-write cannot leave a half-written config.
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(config), StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Could not save {}", file, e);
        }
    }

    /**
     * One-time import of the old single {@code config.json}, where {@code radius} was both the
     * player's preference and the server limit. It seeds {@code server.json} (and {@code client.json}
     * when {@code includeClient}) with the old values that are present, then renames the old file to
     * {@code config.json.migrated}. Existing new-style files are never overwritten.
     * <p>
     * The old file is always moved out of the way: a file without {@code properties} has nothing to import,
     * and a malformed one is kept as {@code config.json.broken}. Either way the import never repeats (and
     * never logs again) on the next start.
     */
    public static void migrateLegacy(boolean includeClient) {
        if (!Files.exists(LEGACY_FILE)) return;
        String suffix = ".migrated";
        try {
            JsonObject root;
            try (Reader reader = Files.newBufferedReader(LEGACY_FILE, StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            JsonElement element = root.get("properties");
            if (element != null && element.isJsonObject()) {
                JsonObject properties = element.getAsJsonObject();
                boolean hasRadius = properties.has("radius");
                int radius = hasRadius ? properties.get("radius").getAsInt() : ClientConfig.DEFAULT_RADIUS;
                if (hasRadius && !Files.exists(SERVER_FILE)) save(SERVER_FILE, new ServerConfig(radius));
                if (includeClient && !Files.exists(CLIENT_FILE)) {
                    int priority = properties.has("priority_type") ? properties.get("priority_type").getAsInt() : 0;
                    int display = properties.has("display_mode") ? properties.get("display_mode").getAsInt() : 0;
                    String sorting = properties.has("sorting_mode") ? properties.get("sorting_mode").getAsString() : "descending";
                    save(CLIENT_FILE, new ClientConfig(radius,
                            priority == 1 ? ClientConfig.PriorityType.ITEM_COUNT : ClientConfig.PriorityType.RANGE,
                            display == 1 ? ClientConfig.DisplayMode.AVAILABLE : ClientConfig.DisplayMode.ALL,
                            "ascending".equals(sorting) ? ClientConfig.SortingMode.ASCENDING : ClientConfig.SortingMode.DESCENDING));
                }
            }
        } catch (IOException | RuntimeException e) {
            // Malformed legacy file (JsonParseException, IllegalStateException, ...): keep it, start fresh
            LOGGER.warn("Could not migrate the old config.json, starting with defaults (old file kept as config.json.broken)", e);
            suffix = ".broken";
        }
        try {
            Files.move(LEGACY_FILE, LEGACY_FILE.resolveSibling("config.json" + suffix), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not rename the old config.json", e);
        }
    }
}
