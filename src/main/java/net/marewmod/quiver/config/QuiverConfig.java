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

    public String _quiver_position_options = "back, leg, force_back";
    public String quiver_position = "back";
    public String _leg_side_options = "left, right";
    public String hip_side = "left";
    public boolean show_strap = true;
    public boolean enable_wiggle = true;
    public boolean render_quiver = true;
    public boolean auto_fill = true;
    public boolean enchantment_glint = true;
    public boolean auto_refill_enchantment = true;
    public java.util.List<String> auto_refill_loot_tables = new java.util.ArrayList<>(java.util.Arrays.asList(
        "minecraft:chests/nether_bridge",
        "minecraft:chests/bastion_treasure"
    ));
    public java.util.List<String> quiver_loot_tables = new java.util.ArrayList<>(java.util.Arrays.asList(
        "minecraft:chests/simple_dungeon",
        "minecraft:chests/stronghold_corridor",
        "minecraft:chests/stronghold_crossing",
        "minecraft:chests/pillager_outpost",
        "minecraft:chests/village/village_fletcher"
    ));

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
        if (instance.quiver_position == null ||
                (!instance.quiver_position.equals("back") && !instance.quiver_position.equals("leg")
                 && !instance.quiver_position.equals("force_back")))
            instance.quiver_position = "back";
        if (instance.hip_side == null) instance.hip_side = new QuiverConfig().hip_side;
        if (instance.auto_refill_loot_tables == null)
            instance.auto_refill_loot_tables = new QuiverConfig().auto_refill_loot_tables;
        if (instance.quiver_loot_tables == null)
            instance.quiver_loot_tables = new QuiverConfig().quiver_loot_tables;
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
