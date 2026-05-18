package net.marewmod.quiver.item;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.marewmod.quiver.QuiverMod;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item QUIVER          = register("quiver",          new QuiverItem(new Item.Settings().maxCount(1)));
    public static final Item COPPER_QUIVER   = register("copper_quiver",   new CopperQuiverItem(new Item.Settings().maxCount(1)));
    public static final Item IRON_QUIVER     = register("iron_quiver",     new TieredQuiverItem(new Item.Settings().maxCount(1), 192));
    public static final Item GOLD_QUIVER     = register("gold_quiver",     new TieredQuiverItem(new Item.Settings().maxCount(1), 256));
    public static final Item DIAMOND_QUIVER  = register("diamond_quiver",  new TieredQuiverItem(new Item.Settings().maxCount(1), 320));
    public static final Item NETHERITE_QUIVER= register("netherite_quiver",new TieredQuiverItem(new Item.Settings().maxCount(1).fireproof(), 384));

    public static final ItemGroup QUIVER_GROUP = Registry.register(
        Registries.ITEM_GROUP,
        new Identifier(QuiverMod.MOD_ID, "quiver"),
        FabricItemGroup.builder()
            .icon(() -> new ItemStack(QUIVER))
            .displayName(Text.translatable("itemGroup.quiver.quiver"))
            .entries((context, entries) -> {
                entries.add(QUIVER);
                entries.add(COPPER_QUIVER);
                entries.add(IRON_QUIVER);
                entries.add(GOLD_QUIVER);
                entries.add(DIAMOND_QUIVER);
                entries.add(NETHERITE_QUIVER);
            })
            .build()
    );

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(QuiverMod.MOD_ID, name), item);
    }

    public static void initialize() {}
}
