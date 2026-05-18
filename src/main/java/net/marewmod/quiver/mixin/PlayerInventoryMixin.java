package net.marewmod.quiver.mixin;

import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.marewmod.quiver.item.QuiverItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    @Shadow public PlayerEntity player;

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"))
    private void tryRouteArrowToQuiver(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArrowItem)) return;
        if (player.getWorld().isClient()) return;
        if (!net.marewmod.quiver.config.QuiverConfig.get().auto_fill) return;

        TrinketsApi.getTrinketComponent(player).ifPresent(comp -> {
            for (var pair : comp.getAllEquipped()) {
                // Use the SlotReference to get the actual slot — pair.getRight() may be a copy
                dev.emi.trinkets.api.SlotReference ref = pair.getLeft();
                ItemStack quiverStack = ref.inventory().getStack(ref.index());
                if (!(quiverStack.getItem() instanceof QuiverItem)) continue;
                int inserted = QuiverItem.insertPublic(quiverStack, stack);
                if (inserted > 0) {
                    stack.decrement(inserted);
                    ref.inventory().markDirty();
                }
                return;
            }
        });
    }
}
