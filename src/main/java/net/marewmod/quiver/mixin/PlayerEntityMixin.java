package net.marewmod.quiver.mixin;

import dev.emi.trinkets.api.TrinketsApi;
import net.marewmod.quiver.QuiverShotTracker;
import net.marewmod.quiver.item.QuiverItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    /**
     * Peeks at the quiver's next arrow WITHOUT consuming it.
     * getProjectileType is called twice per bow shot (draw-check + fire), so we
     * must NOT consume here — BowItemMixin consumes exactly once after firing.
     * We track which quiver to bill via QuiverShotTracker.
     */
    @Inject(method = "getProjectileType", at = @At("RETURN"), cancellable = true)
    private void quiver_peekArrow(ItemStack weapon, CallbackInfoReturnable<ItemStack> cir) {
        QuiverShotTracker.QUIVER.remove();
        QuiverShotTracker.SLOT_REF.remove();

        PlayerEntity self = (PlayerEntity) (Object) this;
        TrinketsApi.getTrinketComponent(self).ifPresent(comp ->
            comp.forEach((ref, stack) -> {
                if (!(stack.getItem() instanceof QuiverItem)) return;
                if (QuiverShotTracker.QUIVER.get() != null) return;

                ItemStack peek = QuiverItem.peekNextArrow(stack);
                if (!peek.isEmpty()) {
                    QuiverShotTracker.QUIVER.set(stack);
                    QuiverShotTracker.SLOT_REF.set(ref);
                    cir.setReturnValue(peek);
                }
            })
        );
    }
}
