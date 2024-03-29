package com.notryken.claimpoints.config;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.notryken.claimpoints.ClaimPoints;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Includes derivative work of code used by
 * <a href="https://github.com/CaffeineMC/sodium-fabric/">Sodium</a>
 */
public class Config {
    private static final String DEFAULT_FILE_NAME = "claimpoints.json";
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .create();

    public static final String DEFAULT_CLAIMPOINT_FORMAT = "Claim (%d)";
    public static final String DEFAULT_CLAIMPOINT_PATTERN = "^Claim \\((\\d+)\\)$";
    public static final String DEFAULT_CLAIMPOINT_ALIAS = "CP";
    public static final String DEFAULT_CLAIMPOINT_COLOR =
            ClaimPoints.waypointColorNames.get(ClaimPoints.waypointColorNames.size() - 1);
    public static final String DEFAULT_FIRST_LINE_PATTERN = "^-?\\d+ blocks from play \\+ -?\\d+ bonus = -?\\d+ total.$";
    public static final String DEFAULT_CLAIM_LINE_PATTERN = "^(.+): x(-?\\d+), z(-?\\d+) \\(-?(\\d+) blocks\\)$";
    public static final List<String> DEFAULT_IGNORED_LINE_PATTERNS = List.of(
            "^Claims:$"
    );
    public static final List<String> DEFAULT_ENDING_LINE_PATTERNS = List.of(
            "^ = -?\\d* blocks left to spend$"
    );

    private static Path configPath;

    public final ClaimPointSettings cpSettings = new ClaimPointSettings();
    public final GriefPreventionSettings gpSettings = new GriefPreventionSettings();

    public static class ClaimPointSettings {
        public String nameFormat = DEFAULT_CLAIMPOINT_FORMAT;
        public String namePattern = DEFAULT_CLAIMPOINT_PATTERN;
        public transient Pattern nameCompiled;
        public String alias = DEFAULT_CLAIMPOINT_ALIAS;
        public String color = DEFAULT_CLAIMPOINT_COLOR;
        public transient int colorIdx;
    }

    public static class GriefPreventionSettings {
        public String firstLinePattern = DEFAULT_FIRST_LINE_PATTERN;
        public transient Pattern firstLineCompiled;
        public String claimLinePattern = DEFAULT_CLAIM_LINE_PATTERN;
        public transient Pattern claimLineCompiled;
        public List<String> ignoredLinePatterns = new ArrayList<>(DEFAULT_IGNORED_LINE_PATTERNS);
        public transient List<Pattern> ignoredLinesCompiled;
        public List<String> endingLinePatterns = new ArrayList<>(DEFAULT_ENDING_LINE_PATTERNS);
        public transient List<Pattern> endingLinesCompiled;
    }

    public void verifyConfig() {
        int indexOfSize = cpSettings.nameFormat.indexOf("%d");
        if (indexOfSize == -1) {
            throw new IllegalArgumentException("Name format '" + cpSettings.nameFormat +
                    "' missing required sequence %d.");
        }
        else {
            cpSettings.namePattern = "^" + Pattern.quote(cpSettings.nameFormat.substring(0, indexOfSize)) +
                    "(\\d+)" + Pattern.quote(cpSettings.nameFormat.substring(indexOfSize + 2)) + "$";
            cpSettings.nameCompiled = Pattern.compile(cpSettings.namePattern);
        }
        if (cpSettings.alias.length() > 2) {
            throw new IllegalArgumentException("Alias '" + cpSettings.alias + "' is longer than 2 characters.");
        }
        cpSettings.colorIdx = ClaimPoints.waypointColorNames.indexOf(cpSettings.color);
        if (cpSettings.colorIdx == -1) {
            throw new IllegalArgumentException("Color '" + cpSettings.color + "' is not a valid waypoint color.");
        }
        gpSettings.firstLineCompiled = Pattern.compile(gpSettings.firstLinePattern);
        gpSettings.claimLineCompiled = Pattern.compile(gpSettings.claimLinePattern);
        gpSettings.ignoredLinesCompiled = new ArrayList<>();
        for (String str : gpSettings.ignoredLinePatterns) {
            gpSettings.ignoredLinesCompiled.add(Pattern.compile(str));
        }
        gpSettings.endingLinesCompiled = new ArrayList<>();
        for (String str : gpSettings.endingLinePatterns) {
            gpSettings.endingLinesCompiled.add(Pattern.compile(str));
        }
    }

    public static @NotNull Config load() {
        Config config = load(DEFAULT_FILE_NAME);

        if (config == null) {
            ClaimPoints.LOG.info("Using default configuration.");
            config = new Config();
            config.verifyConfig();
        }
        else {
            try {
                config.verifyConfig();
            }
            catch (IllegalArgumentException e) {
                ClaimPoints.LOG.warn("Invalid config.", e);
                ClaimPoints.LOG.info("Using default configuration.");
                config = new Config();
                config.verifyConfig();
            }
        }

        config.writeToFile();

        return config;
    }

    public static @Nullable Config load(String name) {
        configPath = Path.of("config").resolve(name);
        Config config = null;

        if (Files.exists(configPath)) {
            try (FileReader reader = new FileReader(configPath.toFile())) {
                config = GSON.fromJson(reader, Config.class);
            } catch (Exception e) {
                ClaimPoints.LOG.error("Unable to load config from file '{}'.", configPath, e);
            }
        } else {
            ClaimPoints.LOG.warn("Unable to locate config file '{}'.", name);
        }
        return config;
    }

    public void writeToFile() {
        Path dir = configPath.getParent();

        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            else if (!Files.isDirectory(dir)) {
                throw new IOException("Not a directory: " + dir);
            }

            // Use a temporary location next to the config's final destination
            Path tempPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");

            // Write the file to the temporary location
            Files.writeString(tempPath, GSON.toJson(this));

            // Atomically replace the old config file (if it exists) with the temporary file
            Files.move(tempPath, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to update config file.", e);
        }
    }
}
