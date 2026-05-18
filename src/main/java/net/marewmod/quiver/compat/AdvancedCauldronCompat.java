package net.marewmod.quiver.compat;

import com.marew.advancedcauldron.block.entity.DyedWaterCauldronBlockEntity;
import com.marew.advancedcauldron.registry.ModBlocks;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.item.DyeableItem;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.marewmod.quiver.item.ModItems;
import net.marewmod.quiver.item.QuiverItem;

public class AdvancedCauldronCompat {

    public static void register() {
        // Register dyeing behavior for all quiver variants
        for (var item : new net.minecraft.item.Item[]{
                ModItems.QUIVER, ModItems.COPPER_QUIVER, ModItems.IRON_QUIVER,
                ModItems.GOLD_QUIVER, ModItems.DIAMOND_QUIVER, ModItems.NETHERITE_QUIVER }) {
            registerDye(item);
            registerUndye(item);
        }
    }

    private static void registerDye(net.minecraft.item.Item item) {
        ModBlocks.DYED_WATER_CAULDRON_BEHAVIOR.put(item, (state, world, pos, player, hand, stack) -> {
            if (!(stack.getItem() instanceof DyeableItem dyeable)) return ActionResult.PASS;
            if (!world.isClient) {
                if (world.getBlockEntity(pos) instanceof DyedWaterCauldronBlockEntity be) {
                    dyeable.setColor(stack, be.getColor());
                    LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
                    world.playSound(null, pos, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
                            SoundCategory.BLOCKS, 1.0f, 1.0f);
                    player.incrementStat(Stats.USE_CAULDRON);
                    player.incrementStat(Stats.USED.getOrCreateStat(item));
                }
            }
            return ActionResult.success(world.isClient);
        });
    }

    private static void registerUndye(net.minecraft.item.Item item) {
        CauldronBehavior.WATER_CAULDRON_BEHAVIOR.put(item, (state, world, pos, player, hand, stack) -> {
            if (!(stack.getItem() instanceof DyeableItem dyeable)) return ActionResult.PASS;
            if (!dyeable.hasColor(stack)) return ActionResult.PASS;
            if (!world.isClient) {
                dyeable.removeColor(stack);
                LeveledCauldronBlock.decrementFluidLevel(state, world, pos);
                world.playSound(null, pos, SoundEvents.ITEM_ARMOR_EQUIP_LEATHER,
                        SoundCategory.BLOCKS, 1.0f, 1.0f);
                player.incrementStat(Stats.USE_CAULDRON);
                player.incrementStat(Stats.USED.getOrCreateStat(item));
            }
            return ActionResult.success(world.isClient);
        });
    }
}
