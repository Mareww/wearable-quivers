package net.marewmod.quiver.mixin;

import net.marewmod.quiver.QuiverShotTracker;
import net.marewmod.quiver.item.QuiverItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BowItem.class)
public class BowItemMixin {

    /** Stores the shooter across the HEAD→REDIRECT injection pair. */
    @Unique
    private static final ThreadLocal<LivingEntity> quiver$currentUser = new ThreadLocal<>();

    /** Capture the shooter at the start of onStoppedUsing so the redirect can use it. */
    @Inject(method = "onStoppedUsing", at = @At("HEAD"))
    private void quiver_captureUser(ItemStack bow, World world, LivingEntity user,
                                     int remainingUseTicks, CallbackInfo ci) {
        quiver$currentUser.set(user);
    }

    /**
     * Intercept the Infinity level check inside onStoppedUsing.
     * Returning 0 for Infinity makes vanilla treat the shot as non-Infinity → arrow consumed.
     * Conservation I  → 25% preserve, 75% consume.
     * Conservation II → 50% preserve, 50% consume.
     */
    @Redirect(method = "onStoppedUsing",
              at = @At(value = "INVOKE",
                       target = "Lnet/minecraft/enchantment/EnchantmentHelper;getLevel(Lnet/minecraft/enchantment/Enchantment;Lnet/minecraft/item/ItemStack;)I"))
    private int quiver_conservationRoll(Enchantment enchantment, ItemStack bow) {
        int level = EnchantmentHelper.getLevel(enchantment, bow);
        if (enchantment != Enchantments.INFINITY || level <= 0) return level;
        LivingEntity user = quiver$currentUser.get();
        if (!(user instanceof PlayerEntity player)) return level;
        if (player.getAbilities().creativeMode) return level;
        // Conservation roll: if fails, return 0 → vanilla treats as no-Infinity → consumes arrow
        float chance = level >= 2 ? 0.5f : 0.25f;
        return player.getRandom().nextFloat() < chance ? level : 0;
    }

    /** After firing: consume from quiver if the arrow came from there. */
    @Inject(method = "onStoppedUsing", at = @At("TAIL"))
    private void quiver_consumeAfterFire(ItemStack bow, World world, LivingEntity user,
                                          int remainingUseTicks, CallbackInfo ci) {
        quiver$currentUser.remove();

        ItemStack quiver = QuiverShotTracker.QUIVER.get();
        dev.emi.trinkets.api.SlotReference slotRef = QuiverShotTracker.SLOT_REF.get();
        QuiverShotTracker.QUIVER.remove();
        QuiverShotTracker.SLOT_REF.remove();

        if (quiver == null || world.isClient()) return;
        if (!(user instanceof PlayerEntity player)) return;
        if (player.getAbilities().creativeMode) return;

        int conservationLevel = EnchantmentHelper.getLevel(Enchantments.INFINITY, bow);
        if (conservationLevel > 0) {
            float chance = conservationLevel >= 2 ? 0.5f : 0.25f;
            if (player.getRandom().nextFloat() < chance) return;
        }

        int elapsed = 72000 - remainingUseTicks;
        float f = elapsed / 20.0f;
        f = (f * f + f * 2.0f) / 3.0f;
        if (Math.min(f, 1.0f) < 0.1f) return;

        if (slotRef != null) {
            ItemStack copy = slotRef.inventory().getStack(slotRef.index()).copy();
            QuiverItem.consumeOneArrow(copy);
            slotRef.inventory().setStack(slotRef.index(), copy);
        } else {
            QuiverItem.consumeOneArrow(quiver);
        }
    }
}
