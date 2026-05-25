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

    @Inject(method = "insertStack(Lnet/minecraft/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void tryRouteArrowToQuiver(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArrowItem)) return;
        if (player.getWorld().isClient()) return;
        if (!net.marewmod.quiver.config.QuiverConfig.get().auto_fill) return;
        if (player.currentScreenHandler instanceof net.minecraft.screen.MerchantScreenHandler) return;

        // Route arrow into the quiver with the most arrows (skip ghosts with 0)
        boolean[] inserted = {false};
        TrinketsApi.getTrinketComponent(player).ifPresent(comp ->
            comp.getAllEquipped().stream()
                .filter(p -> p.getRight().getItem() instanceof QuiverItem)
                .filter(p -> net.minecraft.enchantment.EnchantmentHelper.getLevel(
                    net.marewmod.quiver.QuiverMod.AUTO_REFILL, p.getRight()) > 0)
                .filter(p -> QuiverItem.isAutoRefillActive(p.getRight()))
                .max(java.util.Comparator.comparingInt(p ->
                    QuiverItem.getTotalCount(p.getLeft().inventory().getStack(p.getLeft().index()))))
                .ifPresent(best -> {
                    dev.emi.trinkets.api.SlotReference ref = best.getLeft();
                    ItemStack quiverStack = ref.inventory().getStack(ref.index());
                    int count = QuiverItem.insertPublic(quiverStack, stack);
                    if (count > 0) {
                        stack.decrement(count);
                        QuiverItem.SUPPRESS_EQUIP_SOUND.add(player.getUuid());
                        ref.inventory().markDirty();
                        inserted[0] = true;
                    }
                })
        );

        // If quiver consumed all arrows, return true so the arrow entity discards itself
        if (inserted[0] && stack.isEmpty()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }
}
