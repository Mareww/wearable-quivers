package net.marewmod.quiver.mixin;

import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.marewmod.quiver.item.QuiverItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @Shadow private int pickupDelay;

    @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
    private void tryVacuumArrowIntoQuiver(PlayerEntity player, CallbackInfo ci) {
        if (this.pickupDelay != 0) return;
        if (!net.marewmod.quiver.config.QuiverConfig.get().auto_fill) return;

        ItemEntity self = (ItemEntity)(Object)this;
        ItemStack entityStack = self.getStack();

        if (!(entityStack.getItem() instanceof ArrowItem)) return;
        if (self.getWorld().isClient()) return;

        TrinketsApi.getTrinketComponent(player).ifPresent(comp -> {
            for (var pair : comp.getAllEquipped()) {
                // Use the SlotReference to get the actual slot — pair.getRight() may be a copy
                dev.emi.trinkets.api.SlotReference ref = pair.getLeft();
                ItemStack quiverStack = ref.inventory().getStack(ref.index());
                if (!(quiverStack.getItem() instanceof QuiverItem)) continue;

                int space = QuiverItem.getMaxCapacity(quiverStack) - QuiverItem.getTotalCount(quiverStack);
                if (space <= 0) return;

                ItemStack toInsert = entityStack.copy();
                toInsert.setCount(Math.min(entityStack.getCount(), space));

                net.marewmod.quiver.item.QuiverItem.SUPPRESS_EQUIP_SOUND.add(player.getUuid());
                int inserted = QuiverItem.insertPublic(quiverStack, toInsert);
                if (inserted <= 0) return;

                ref.inventory().markDirty(); // persist the change to the actual slot
                entityStack.decrement(inserted);
                if (entityStack.isEmpty()) self.discard();

                player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sound.SoundEvents.ENTITY_ITEM_PICKUP,
                        net.minecraft.sound.SoundCategory.PLAYERS,
                        0.2f, (float)((player.getRandom().nextDouble() - player.getRandom().nextDouble()) * 1.4 + 2.0));
                player.incrementStat(net.minecraft.stat.Stats.PICKED_UP.getOrCreateStat(entityStack.getItem()));

                ci.cancel();
                return;
            }
        });
    }
}
