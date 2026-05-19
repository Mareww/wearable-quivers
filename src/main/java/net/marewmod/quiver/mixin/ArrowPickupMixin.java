package net.marewmod.quiver.mixin;

import dev.emi.trinkets.api.TrinketsApi;
import net.marewmod.quiver.item.QuiverItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PersistentProjectileEntity.class)
public abstract class ArrowPickupMixin {

    @Shadow protected abstract ItemStack asItemStack();

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void tryVacuumArrowIntoQuiver(PlayerEntity player, CallbackInfo ci) {
        if (!net.marewmod.quiver.config.QuiverConfig.get().auto_fill) return;
        if (player.getWorld().isClient()) return;

        PersistentProjectileEntity self = (PersistentProjectileEntity)(Object)this;
        ItemStack arrowStack = asItemStack();
        if (!(arrowStack.getItem() instanceof ArrowItem)) return;

        boolean[] inserted = {false};
        TrinketsApi.getTrinketComponent(player).ifPresent(comp -> {
            for (var pair : comp.getAllEquipped()) {
                dev.emi.trinkets.api.SlotReference ref = pair.getLeft();
                ItemStack quiverStack = ref.inventory().getStack(ref.index());
                if (!(quiverStack.getItem() instanceof QuiverItem)) continue;

                int space = QuiverItem.getMaxCapacity(quiverStack) - QuiverItem.getTotalCount(quiverStack);
                if (space <= 0) continue;

                ItemStack toInsert = arrowStack.copy();
                toInsert.setCount(Math.min(arrowStack.getCount(), space));

                int count = QuiverItem.insertPublic(quiverStack, toInsert);
                if (count <= 0) continue;

                ref.inventory().markDirty();
                player.sendPickup(self, count);
                self.discard();
                inserted[0] = true;
                ci.cancel();
                return;
            }
        });
    }
}
