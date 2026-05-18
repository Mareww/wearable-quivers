package net.marewmod.quiver.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.marewmod.quiver.QuiverShotTracker;
import net.marewmod.quiver.item.QuiverItem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {

    @Inject(method = "loadProjectiles", at = @At("TAIL"))
    private static void consumeQuiverArrowOnLoad(
            LivingEntity shooter, ItemStack crossbow,
            CallbackInfoReturnable<Boolean> cir) {
        // Only server-side, only when loading succeeded, only for players
        if (shooter.getWorld().isClient()) return;
        if (!cir.getReturnValue()) return;
        if (!(shooter instanceof PlayerEntity player)) return;
        if (player.getAbilities().creativeMode) return;

        // Conservation (reworked Infinity): level 1 = 25%, level 2 = 50% chance to preserve
        ItemStack crossbowStack = player.getMainHandStack().getItem() instanceof net.minecraft.item.CrossbowItem
            ? player.getMainHandStack() : player.getOffHandStack();
        int conservationLevel = EnchantmentHelper.getLevel(Enchantments.INFINITY, crossbowStack);
        if (conservationLevel > 0) {
            float chance = conservationLevel >= 2 ? 0.5f : 0.25f;
            if (player.getRandom().nextFloat() < chance) {
                QuiverShotTracker.QUIVER.remove();
                return;
            }
        }

        dev.emi.trinkets.api.SlotReference ref = QuiverShotTracker.SLOT_REF.get();
        ItemStack quiverStack = QuiverShotTracker.QUIVER.get();
        QuiverShotTracker.QUIVER.remove();
        QuiverShotTracker.SLOT_REF.remove();

        if (ref != null) {
            ItemStack copy = ref.inventory().getStack(ref.index()).copy();
            QuiverItem.consumeOneArrow(copy);
            ref.inventory().setStack(ref.index(), copy);
        } else if (quiverStack != null && !quiverStack.isEmpty() &&
                quiverStack.getItem() instanceof QuiverItem) {
            QuiverItem.consumeOneArrow(quiverStack);
        }
    }
}
