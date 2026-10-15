package com.kingtalon23.nakedhyfraid.hardcore;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Players who have used up their one life.
 *
 * <p>Stored as one {@code uuid<TAB>username} line per player inside the universe (save) folder, so each
 * world save keeps its own list: dying in one singleplayer save does not lock you out of another.
 */
public class DeadPlayerStore {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Path file;
    private final Map<UUID, String> dead = new LinkedHashMap<>();

    public DeadPlayerStore(Path file) {
        this.file = file;
    }

    public synchronized void load() {
        dead.clear();
        if (!Files.exists(file)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("\t", 2);
                try {
                    dead.put(UUID.fromString(parts[0]), parts.length > 1 ? parts[1] : "");
                } catch (IllegalArgumentException e) {
                    LOGGER.at(Level.WARNING).log("Skipping bad line in %s: %s", file, line);
                }
            }
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to read %s", file);
        }
    }

    public synchronized boolean isDead(UUID uuid) {
        return dead.containsKey(uuid);
    }

    public synchronized void markDead(UUID uuid, String username) {
        dead.put(uuid, username != null ? username : "");
        save();
    }

    /**
     * Removes a player by username (case-insensitive) or UUID string.
     *
     * @return the removed player's username, or {@code null} if nobody matched
     */
    public synchronized String revive(String nameOrUuid) {
        UUID match = null;
        for (Map.Entry<UUID, String> entry : dead.entrySet()) {
            if (entry.getKey().toString().equalsIgnoreCase(nameOrUuid)
                    || entry.getValue().toLowerCase(Locale.ROOT).equals(nameOrUuid.toLowerCase(Locale.ROOT))) {
                match = entry.getKey();
                break;
            }
        }
        if (match == null) {
            return null;
        }
        String name = dead.remove(match);
        save();
        return name.isEmpty() ? match.toString() : name;
    }

    public synchronized List<String> list() {
        List<String> out = new ArrayList<>();
        dead.forEach((uuid, name) -> out.add(name.isEmpty() ? uuid.toString() : name + " (" + uuid + ")"));
        return out;
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            StringBuilder sb = new StringBuilder("# Naked & Hyfraid: players who died in hardcore. Use /hardcore revive <name> to remove one.\n");
            dead.forEach((uuid, name) -> sb.append(uuid).append('\t').append(name).append('\n'));
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).withCause(e).log("Failed to write %s", file);
        }
    }
}
