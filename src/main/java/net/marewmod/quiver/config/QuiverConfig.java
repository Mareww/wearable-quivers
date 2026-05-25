package net.marewmod.quiver.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.marewmod.quiver.QuiverMod;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuiverConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static QuiverConfig instance;

    public static class LootEntry {
        public String table  = "";
        public int    chance = 10; // percent (e.g. 12 = 12%)
        public LootEntry() {}
        public LootEntry(String table, int chance) { this.table = table; this.chance = chance; }
    }

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

    public List<LootEntry> auto_refill_loot_entries = new ArrayList<>(Arrays.asList(
        new LootEntry("minecraft:chests/nether_bridge",    6),
        new LootEntry("minecraft:chests/bastion_treasure", 6)
    ));
    public List<LootEntry> quiver_loot_entries = new ArrayList<>(Arrays.asList(
        new LootEntry("minecraft:chests/simple_dungeon",           12),
        new LootEntry("minecraft:chests/stronghold_corridor",      12),
        new LootEntry("minecraft:chests/stronghold_crossing",      12),
        new LootEntry("minecraft:chests/pillager_outpost",         12),
        new LootEntry("minecraft:chests/village/village_fletcher", 12)
    ));

    // Legacy fields — kept for migration from old config format, nulled out on save
    public List<String> auto_refill_loot_tables = null;
    public List<String> quiver_loot_tables       = null;

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

        // Migrate old string lists to entry lists
        if ((instance.auto_refill_loot_entries == null || instance.auto_refill_loot_entries.isEmpty())
                && instance.auto_refill_loot_tables != null && !instance.auto_refill_loot_tables.isEmpty()) {
            instance.auto_refill_loot_entries = new ArrayList<>();
            for (String t : instance.auto_refill_loot_tables)
                instance.auto_refill_loot_entries.add(new LootEntry(t, 6));
        }
        if ((instance.quiver_loot_entries == null || instance.quiver_loot_entries.isEmpty())
                && instance.quiver_loot_tables != null && !instance.quiver_loot_tables.isEmpty()) {
            instance.quiver_loot_entries = new ArrayList<>();
            for (String t : instance.quiver_loot_tables)
                instance.quiver_loot_entries.add(new LootEntry(t, 12));
        }

        // Null out legacy fields so they don't persist in future saves
        instance.auto_refill_loot_tables = null;
        instance.quiver_loot_tables       = null;

        if (instance.quiver_position == null ||
                (!instance.quiver_position.equals("back") && !instance.quiver_position.equals("leg")
                 && !instance.quiver_position.equals("force_back")))
            instance.quiver_position = "back";
        if (instance.hip_side == null) instance.hip_side = new QuiverConfig().hip_side;
        if (instance.auto_refill_loot_entries == null)
            instance.auto_refill_loot_entries = new QuiverConfig().auto_refill_loot_entries;
        if (instance.quiver_loot_entries == null)
            instance.quiver_loot_entries = new QuiverConfig().quiver_loot_entries;

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
