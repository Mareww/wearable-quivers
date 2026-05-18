package net.marewmod.quiver.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.InfinityEnchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class InfinityEnchantmentMixin {

    @Inject(method = "getMaxLevel", at = @At("HEAD"), cancellable = true)
    private void raiseInfinityMaxLevel(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof InfinityEnchantment) {
            cir.setReturnValue(2);
        }
    }
}
