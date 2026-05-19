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

    public static final net.minecraft.enchantment.Enchantment AUTO_REFILL =
        net.minecraft.registry.Registry.register(
            net.minecraft.registry.Registries.ENCHANTMENT,
            new Identifier(MOD_ID, "auto_refill"),
            new net.marewmod.quiver.enchantment.AutoRefillEnchantment());

    /** Client → server: scroll over a quiver slot to change selected arrow type. */
    public static final Identifier QUIVER_SCROLL_PACKET = new Identifier(MOD_ID, "scroll");

    /** Server → client: update the SelectedSlot of the equipped quiver directly. */
    public static final Identifier QUIVER_SYNC_PACKET = new Identifier(MOD_ID, "sync_slot");

    /** Client → server: toggle Auto Refill active/paused on the hovered quiver. */
    public static final Identifier QUIVER_TOGGLE_REFILL = new Identifier(MOD_ID, "toggle_refill");


    @Override
    public void onInitialize() {
        ModItems.initialize();
        QuiverSmithingRecipe.SERIALIZER.getClass(); // force static init / registration

        if (FabricLoader.getInstance().isModLoaded("advancedcauldron")) {
            AdvancedCauldronCompat.register();
        }

        ServerPlayNetworking.registerGlobalReceiver(QUIVER_SCROLL_PACKET,
            (server, player, handler, buf, responseSender) -> {
                int direction  = buf.readInt();
                String reqGroup = buf.readString(); // exact slot group the user scrolled over ("" = any)
                String reqName  = buf.readString(); // exact slot name  ("" = any)
                server.execute(() -> {
                    boolean found = dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(player).map(comp -> {
                        var allQuivers = comp.getAllEquipped().stream()
                            .filter(p -> p.getRight().getItem() instanceof QuiverItem)
                            .toList();
                        if (allQuivers.isEmpty()) return false;

                        // If the quiver is in the player's regular inventory, any Trinkets quiver
                        // is a stale ghost from a component sync — clear it and fall back to inventory
                        boolean quiverInInventory = false;
                        for (int i = 0; i < player.getInventory().size(); i++) {
                            if (player.getInventory().getStack(i).getItem() instanceof QuiverItem) {
                                quiverInInventory = true; break;
                            }
                        }
                        if (quiverInInventory) {
                            allQuivers.forEach(p -> p.getLeft().inventory().setStack(p.getLeft().index(), ItemStack.EMPTY));
                            return false; // fall through to inventory fallback
                        }

                        // Find the target slot:
                        // – if the client told us exactly which slot (inventory scroll), use that
                        // – otherwise (in-world scroll) prefer legs, then chest
                        dev.emi.trinkets.api.SlotReference target = null;
                        if (!reqGroup.isEmpty()) {
                            for (var p : allQuivers) {
                                var st = p.getLeft().inventory().getSlotType();
                                if (st.getGroup().equals(reqGroup) && st.getName().equals(reqName)) {
                                    target = p.getLeft(); break;
                                }
                            }
                        }
                        if (target == null) {
                            // Prefer legs/quiver; fall back to chest/back
                            for (var p : allQuivers) {
                                target = p.getLeft();
                                if (target.inventory().getSlotType().getGroup().equals("legs")) break;
                            }
                        }
                        if (target == null) return false;

                        // Clear every OTHER quiver slot (ghost cleanup)
                        final dev.emi.trinkets.api.SlotReference keep = target;
                        for (var p : allQuivers) {
                            var ref2 = p.getLeft();
                            if (ref2.inventory() == keep.inventory() && ref2.index() == keep.index()) continue;
                            ref2.inventory().setStack(ref2.index(), ItemStack.EMPTY);
                        }

                        ItemStack live = keep.inventory().getStack(keep.index());
                        int n = QuiverItem.getSlotCount(live);
                        if (n <= 1) return allQuivers.size() > 1; // deduped but nothing to scroll
                        int cur  = QuiverItem.getSelectedSlot(live);
                        int next = Math.max(0, Math.min(n - 1, cur + direction));
                        ItemStack updated = live.copy();
                        QuiverItem.setSelectedSlot(updated, next);
                        QuiverItem.SUPPRESS_EQUIP_SOUND.add(player.getUuid());
                        keep.inventory().setStack(keep.index(), updated);
                        if (player instanceof net.minecraft.server.network.ServerPlayerEntity) {
                            net.minecraft.server.network.ServerPlayerEntity sp = (net.minecraft.server.network.ServerPlayerEntity) player;
                            var b2 = PacketByteBufs.create();
                            b2.writeInt(next);
                            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(sp, QUIVER_SYNC_PACKET, b2);
                        }
                        return true;
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


        // Toggle Auto Refill (T key from client)
        ServerPlayNetworking.registerGlobalReceiver(QUIVER_TOGGLE_REFILL,
            (server, player, handler, buf, responseSender) -> server.execute(() -> {
                boolean toggled = dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(player)
                    .map(comp -> {
                        for (var pair : comp.getAllEquipped()) {
                            ItemStack s = pair.getLeft().inventory().getStack(pair.getLeft().index());
                            if (!(s.getItem() instanceof QuiverItem)) continue;
                            if (net.minecraft.enchantment.EnchantmentHelper.getLevel(AUTO_REFILL, s) <= 0) continue;
                            ItemStack copy = s.copy();
                            QuiverItem.toggleAutoRefill(copy);
                            QuiverItem.SUPPRESS_EQUIP_SOUND.add(player.getUuid());
                            pair.getLeft().inventory().setStack(pair.getLeft().index(), copy);
                            return true;
                        }
                        return false;
                    }).orElse(false);
                if (!toggled) {
                    for (int i = 0; i < player.getInventory().size(); i++) {
                        ItemStack s = player.getInventory().getStack(i);
                        if (!(s.getItem() instanceof QuiverItem)) continue;
                        if (net.minecraft.enchantment.EnchantmentHelper.getLevel(AUTO_REFILL, s) <= 0) continue;
                        QuiverItem.toggleAutoRefill(s);
                        player.getInventory().markDirty();
                        break;
                    }
                }
            }));

        // AutoRefill tick — pull arrows from inventory into the equipped quiver (every second)
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerManager().getPlayerList()) {
                if (player.age % 20 != 0) continue;
                dev.emi.trinkets.api.TrinketsApi.getTrinketComponent(player).ifPresent(comp ->
                    comp.getAllEquipped().stream()
                        .filter(p -> p.getRight().getItem() instanceof QuiverItem)
                        .max(java.util.Comparator.comparingInt(p ->
                            QuiverItem.getTotalCount(p.getLeft().inventory().getStack(p.getLeft().index()))))
                        .ifPresent(pair -> {
                            dev.emi.trinkets.api.SlotReference ref = pair.getLeft();
                            ItemStack quiverStack = ref.inventory().getStack(ref.index());
                            if (net.minecraft.enchantment.EnchantmentHelper.getLevel(AUTO_REFILL, quiverStack) <= 0) return;
                            if (!QuiverItem.isAutoRefillActive(quiverStack)) return;
                            int space = QuiverItem.getMaxCapacity(quiverStack) - QuiverItem.getTotalCount(quiverStack);
                            if (space <= 0) return;
                            boolean any = false;
                            for (int i = 0; i < player.getInventory().size() && space > 0; i++) {
                                ItemStack inv = player.getInventory().getStack(i);
                                if (!(inv.getItem() instanceof net.minecraft.item.ArrowItem)) continue;
                                int inserted = QuiverItem.insertPublic(quiverStack, inv);
                                if (inserted <= 0) continue;
                                inv.decrement(inserted);
                                space -= inserted;
                                any = true;
                            }
                            if (any) {
                                QuiverItem.SUPPRESS_EQUIP_SOUND.add(player.getUuid());
                                ref.inventory().markDirty();
                            }
                        })
                );
            }
        });

        // AutoRefill enchanted book in nether fortress + bastion, and master fletcher trade
        LootTableEvents.MODIFY.register((rm, manager, id, tableBuilder, source) -> {
            if (!source.isBuiltin()) return;
            String s = id.toString();
            if (s.equals("minecraft:chests/nether_bridge") || s.equals("minecraft:chests/bastion_treasure")) {
                net.minecraft.item.ItemStack book = net.minecraft.item.Items.ENCHANTED_BOOK.getDefaultStack();
                net.minecraft.item.EnchantedBookItem.addEnchantment(book,
                    new net.minecraft.enchantment.EnchantmentLevelEntry(AUTO_REFILL, 1));
                tableBuilder.pool(LootPool.builder()
                    .rolls(ConstantLootNumberProvider.create(1))
                    .with(ItemEntry.builder(net.minecraft.item.Items.ENCHANTED_BOOK)
                        .apply(net.minecraft.loot.function.SetNbtLootFunction.builder(book.getNbt())))
                    .conditionally(RandomChanceLootCondition.builder(0.06f)));
            }
        });

        net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper.registerVillagerOffers(
            net.minecraft.village.VillagerProfession.FLETCHER, 5,
            factories -> factories.add((entity, random) -> {
                net.minecraft.item.ItemStack book = net.minecraft.item.Items.ENCHANTED_BOOK.getDefaultStack();
                net.minecraft.item.EnchantedBookItem.addEnchantment(book,
                    new net.minecraft.enchantment.EnchantmentLevelEntry(AUTO_REFILL, 1));
                return new net.minecraft.village.TradeOffer(
                    new ItemStack(net.minecraft.item.Items.EMERALD, 24), book, 1, 30, 0.05f);
            }));

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
