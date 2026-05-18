package net.marewmod.quiver.mixin;

import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.recipe.ArmorDyeRecipe;
import net.minecraft.registry.DynamicRegistryManager;
import net.marewmod.quiver.item.QuiverItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorDyeRecipe.class)
public class ArmorDyeRecipeMixin {

    @Inject(method = "craft", at = @At("HEAD"))
    private void clearDefaultBrownBeforeBlend(RecipeInputInventory inventory, DynamicRegistryManager registryManager, CallbackInfoReturnable<ItemStack> cir) {
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!(stack.getItem() instanceof QuiverItem)) continue;
            NbtCompound display = stack.getSubNbt("display");
            if (display != null && display.contains("color")) {
                if (display.getInt("color") == DyeableItem.DEFAULT_COLOR) {
                    display.remove("color");
                }
            }
            break;
        }
    }
}
