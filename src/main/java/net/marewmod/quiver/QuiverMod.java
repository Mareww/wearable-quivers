package net.marewmod.quiver;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.loot.v2.LootTableEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.condition.RandomChanceLootCondition;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.marewmod.quiver.recipe.QuiverSmithingRecipe;
import net.marewmod.quiver.compat.AdvancedCauldronCompat;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.marewmod.quiver.item.ModItems;
import net.marewmod.quiver.item.QuiverItem;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QuiverMod implements ModInitializer {

    public static final String MOD_ID = "quiver";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Client → server: scroll over a quiver slot to change selected arrow type. */
    public static final Identifier QUIVER_SCROLL_PACKET = new Identifier(MOD_ID, "scroll");

    /** Server → client: update the SelectedSlot of the equipped quiver directly. */
    public static final Identifier QUIVER_SYNC_PACKET = new Identifier(MOD_ID, "sync_slot");


    @Override
    public void onInitialize() {
        ModItems.initialize();
        QuiverSmithingRecipe.SERIALIZER.getClass(); // force static init / registration

        if (FabricLoader.getInstance().isModLoaded("advancedcauldron")) {
            AdvancedCauldronCompat.register();
        }

        ServerPlayNetworking.registerGlobalReceiver(QUIVER_SCROLL_PACKET,
            (server, player, handler, buf, responseSender) -> {
                int direction = buf.readInt();
                buf.readInt(); // slot ID no longer used — find quiver directly
                server.execute(() -> {
                    QuiverItem.SUPPRESS_EQUIP_SOUND.add(player.getUuid());
                    
                    // Find the quiver in Trinkets slots first (equipped)
                    boolean found = dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(player).map(comp -> {
                        for (var groupEntry : comp.getInventory().entrySet()) {
                            for (var slotEntry : groupEntry.getValue().entrySet()) {
                                var inv = slotEntry.getValue();
                                for (int i = 0; i < inv.size(); i++) {
                                    ItemStack s = inv.getStack(i);
                                    if (!(s.getItem() instanceof QuiverItem)) continue;
                                    int n = QuiverItem.getSlotCount(s);
                                    if (n <= 1) continue;
                                    int cur = QuiverItem.getSelectedSlot(s);
                                    int next = Math.max(0, Math.min(n - 1, cur + direction));
                                    ItemStack updated = s.copy();
                                    QuiverItem.setSelectedSlot(updated, next);
                                    inv.setStack(i, updated);
                                    if (player instanceof net.minecraft.server.network.ServerPlayerEntity) {
                                        net.minecraft.server.network.ServerPlayerEntity sp = (net.minecraft.server.network.ServerPlayerEntity) player;
                                        var buf2 = PacketByteBufs.create();
                                        buf2.writeInt(next);
                                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sp, QUIVER_SYNC_PACKET, buf2);
                                    }
                                    return true;
                                }
                            }
                        }
                        return false;
                    }).orElse(false);

                    // Fallback: quiver is in the player's inventory (not equipped)
                    if (!found) {
                        for (int i = 0; i < player.getInventory().size(); i++) {
                            ItemStack s = player.getInventory().getStack(i);
                            if (!(s.getItem() instanceof QuiverItem)) continue;
                            int n = QuiverItem.getSlotCount(s);
                            if (n <= 1) continue;
                            int next = (QuiverItem.getSelectedSlot(s) + direction + n) % n;
                            QuiverItem.setSelectedSlot(s, next);
                            player.getInventory().markDirty();
                            break;
                        }
                    }

                    player.playerScreenHandler.sendContentUpdates();
                });
            });


        // Add quivers to loot tables
        LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
            if (!source.isBuiltin()) return;
            String idStr = id.toString();
            boolean isChest = idStr.equals("minecraft:chests/simple_dungeon")
                || idStr.equals("minecraft:chests/stronghold_corridor")
                || idStr.equals("minecraft:chests/stronghold_crossing")
                || idStr.equals("minecraft:chests/pillager_outpost")
                || idStr.equals("minecraft:chests/village/village_fletcher");
            if (!isChest) return;
            tableBuilder.pool(LootPool.builder()
                .rolls(ConstantLootNumberProvider.create(1))
                .with(ItemEntry.builder(ModItems.QUIVER).weight(1))
                .conditionally(RandomChanceLootCondition.builder(0.12f)));
        });

        LOGGER.info("Wearable Quivers loaded.");
    }
}
