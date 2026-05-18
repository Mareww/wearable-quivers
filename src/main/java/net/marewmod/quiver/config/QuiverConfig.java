package net.marewmod.quiver.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.marewmod.quiver.QuiverMod;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class QuiverConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static QuiverConfig instance;

    public String _leg_side_options = "left, right";
    public String hip_side = "left";
    public boolean show_strap = true;
    public boolean enable_wiggle = true;
    public boolean render_quiver = true;
    public boolean auto_fill = true;
    public boolean auto_switch = true;

    public boolean isHipLeft() {
        return !"right".equals(hip_side);
    }

    public static QuiverConfig get() {
        if (instance == null) load();
        return instance;
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("wearable_quivers.json");
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file)) {
                instance = GSON.fromJson(r, QuiverConfig.class);
                if (instance == null) instance = new QuiverConfig();
            } catch (Exception e) {
                QuiverMod.LOGGER.error("Failed to load config", e);
                instance = new QuiverConfig();
            }
        } else {
            instance = new QuiverConfig();
        }
        if (instance.hip_side == null) instance.hip_side = new QuiverConfig().hip_side;
        save();
    }

    public static void save() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("wearable_quivers.json");
        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file)) {
                GSON.toJson(instance, w);
            }
        } catch (Exception e) {
            QuiverMod.LOGGER.error("Failed to save config", e);
        }
    }
}
